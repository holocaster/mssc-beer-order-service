package guru.sfg.beer.order.service.services;

import br.com.prcompany.beerevents.model.BeerOrderDTO;
import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = this.beerOrderRepository.saveAndFlush(beerOrder);

        this.sendEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        this.awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATION_PENDING);
        this.beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            if (isValid) {
                this.sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);

                BeerOrder validateBeerOrder = this.beerOrderRepository.findById(beerOrderId).get();
                this.sendEvent(validateBeerOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
            } else {
                this.sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }

        }, () -> log.error("Order not found for id {}", beerOrderId));
    }

    @Transactional
    @Override
    public void beerOrderAllocation(BeerOrderDTO beerOrderDTO, BeerOrderEventEnum beerOrderEventEnum, BeerOrderStatusEnum beerOrderStatusEnum) {
        BeerOrder beerOrder = this.beerOrderRepository.getOne(beerOrderDTO.getId());
        this.sendEvent(beerOrder, beerOrderEventEnum);
        if (beerOrderStatusEnum != null) {
            awaitForStatus(beerOrder.getId(), beerOrderStatusEnum);
        }
        if (beerOrderEventEnum != BeerOrderEventEnum.ALLOCATION_FAILED) {
            this.updateAllocatedQty(beerOrderDTO);
        }
    }

    @Override
    public void beerOrderPickedUp(UUID id) {
        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(id);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            this.sendEvent(beerOrder, BeerOrderEventEnum.BEER_ORDER_PICKED_UP);
        }, () -> log.error("Order not found for id {}", id));
    }

    @Override
    public void cancelOrder(UUID orderId) {
        this.beerOrderRepository.findById(orderId).ifPresentOrElse(beerOrder -> {
            this.sendEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
        }, () -> log.error("Order not found for id {}", orderId));
    }

    private void updateAllocatedQty(BeerOrderDTO beerOrderDTO) {
        BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDTO.getId());

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDTO.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(allocatedOrder);
    }

    private void sendEvent(BeerOrder beerOrder, BeerOrderEventEnum beerOrderEventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = this.build(beerOrder);

        Message msg = MessageBuilder.withPayload(beerOrderEventEnum)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        sm.sendEvent(msg);
    }

    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order Found");
                } else {
                    log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderStatus().name());
                }
            }, () -> {
                log.debug("Order Id Not Found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(200);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }


    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = this.stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(this.beerOrderStateChangeInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
        });

        sm.start();

        return sm;
    }
}

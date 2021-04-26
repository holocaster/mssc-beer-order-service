package guru.sfg.beer.order.service.sm.actions;

import br.com.prcompany.beerevents.events.DeallocateOrderRequest;
import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import br.com.prcompany.beerevents.utils.EventsConstants;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeallocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        log.debug("deallocate order was called");

        String id = (String) context.getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER);

        log.debug("Sending deallocation as jms message for {}", id);
        BeerOrder beerOrder = this.beerOrderRepository.getOne(UUID.fromString(id));

        this.jmsTemplate.convertAndSend(EventsConstants.DEALLOCATE_ORDER_QUEUE, DeallocateOrderRequest.builder()
                .beerOrderDTO(this.beerOrderMapper.beerOrderToEventDto(beerOrder)).build());
    }
}

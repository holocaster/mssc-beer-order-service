package guru.sfg.beer.order.service.services.testcomponents;

import br.com.prcompany.beerevents.events.AllocateOrderRequest;
import br.com.prcompany.beerevents.events.AllocateOrderResult;
import br.com.prcompany.beerevents.utils.EventsConstants;
import guru.sfg.beer.order.service.services.BeerOrderManagerImplIT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = EventsConstants.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {

        AllocateOrderRequest allocateOrderRequest = (AllocateOrderRequest) msg.getPayload();

        boolean allocationError = false;
        boolean pendingInventory = false;

        if (BeerOrderManagerImplIT.FAIL_ALLOCATION.equals(allocateOrderRequest.getBeerOrderDTO().getCustomerRef())) {
            allocationError = true;
        }
        if (BeerOrderManagerImplIT.PARTIAL_ALLOCATION.equals(allocateOrderRequest.getBeerOrderDTO().getCustomerRef())) {
            pendingInventory = true;
        }

        boolean finalPendingInventory = pendingInventory;
        allocateOrderRequest.getBeerOrderDTO().getBeerOrderLines().forEach(line -> {
            if (finalPendingInventory) {
                line.setQuantityAllocated(line.getOrderQuantity() - 1);
            } else {
                line.setQuantityAllocated(line.getOrderQuantity());
            }
        });
        this.jmsTemplate.convertAndSend(EventsConstants.ALLOCATE_ORDER_RESULT_QUEUE, AllocateOrderResult.builder()
                .beerOrderDTO(allocateOrderRequest.getBeerOrderDTO())
                .allocationError(allocationError)
                .pendingInventory(pendingInventory)
                .build());
    }
}

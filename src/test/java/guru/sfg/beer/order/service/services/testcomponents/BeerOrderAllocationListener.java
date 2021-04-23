package guru.sfg.beer.order.service.services.testcomponents;

import br.com.prcompany.beerevents.events.AllocateOrderRequest;
import br.com.prcompany.beerevents.events.AllocateOrderResult;
import br.com.prcompany.beerevents.utils.EventsConstants;
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

        allocateOrderRequest.getBeerOrderDTO().getBeerOrderLines().forEach(line -> {
            line.setQuantityAllocated(line.getOrderQuantity());
        });
        this.jmsTemplate.convertAndSend(EventsConstants.ALLOCATE_ORDER_RESULT_QUEUE, AllocateOrderResult.builder()
                .beerOrderDTO(allocateOrderRequest.getBeerOrderDTO())
                .build());
    }
}

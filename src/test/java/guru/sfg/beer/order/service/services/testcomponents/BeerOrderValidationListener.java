package guru.sfg.beer.order.service.services.testcomponents;

import br.com.prcompany.beerevents.events.ValidateBeerOrderRequest;
import br.com.prcompany.beerevents.events.ValidateBeerOrderResult;
import br.com.prcompany.beerevents.utils.EventsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = EventsConstants.VALIDATE_ORDER_QUEUE)
    public void listen(Message msg) {

        ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();

        this.jmsTemplate.convertAndSend(EventsConstants.VALIDATE_ORDER_RESULT_QUEUE,
                ValidateBeerOrderResult.builder()
                        .isValid(true)
                        .orderId(request.getBeerOrderDTO().getId()).build());
    }
}

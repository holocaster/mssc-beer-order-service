package guru.sfg.beer.order.service.sm.actions;

import br.com.prcompany.beerevents.events.AllocateFailRequest;
import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import br.com.prcompany.beerevents.utils.EventsConstants;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        String id = (String) context.getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER);
        log.error("Allocation failed. Sending notification for id {}", id);

        this.jmsTemplate.convertAndSend(EventsConstants.ALLOCATE_FAIL_QUEUE,
                AllocateFailRequest.builder().uuid(UUID.fromString(id)).build());


    }
}

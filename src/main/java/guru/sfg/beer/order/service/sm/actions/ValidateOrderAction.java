package guru.sfg.beer.order.service.sm.actions;

import br.com.prcompany.beerevents.events.ValidateBeerOrderRequest;
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

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        log.debug("Validate order was called");

        Optional.ofNullable((String) context.getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER)).ifPresent(id -> {
            log.debug("Sending validation as jms message for {}", id);
            BeerOrder beerOrder = this.beerOrderRepository.getOne(UUID.fromString(id));
            this.jmsTemplate.convertAndSend(EventsConstants.VALIDATE_ORDER_QUEUE, ValidateBeerOrderRequest.builder().beerOrderDTO(this.beerOrderMapper.beerOrderToEventDto(beerOrder)).build());
        });
    }
}

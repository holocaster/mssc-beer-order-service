package guru.sfg.beer.order.service.sm;

import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.sm.actions.AllocateOrderAction;
import guru.sfg.beer.order.service.sm.actions.AllocationFailureAction;
import guru.sfg.beer.order.service.sm.actions.ValidateOrderAction;
import guru.sfg.beer.order.service.sm.actions.ValidationFailureAction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    @Autowired
    private ValidateOrderAction validateOrderAction;

    @Autowired
    private AllocateOrderAction allocateOrderAction;

    @Autowired
    private ValidationFailureAction validationFailureAction;

    @Autowired
    private AllocationFailureAction allocationFailureAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
        states.withStates().
                initial(BeerOrderStatusEnum.NEW)
                .states(EnumSet.allOf(BeerOrderStatusEnum.class))
                .end(BeerOrderStatusEnum.PICKED_UP)
                .end(BeerOrderStatusEnum.DELIVERED)
                .end(BeerOrderStatusEnum.CANCELED)
                .end(BeerOrderStatusEnum.DELIVERY_EXCEPTION)
                .end(BeerOrderStatusEnum.VALIDATION_EXCEPTION)
                .end(BeerOrderStatusEnum.ALLOCATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.VALIDATION_PENDING).event(BeerOrderEventEnum.VALIDATE_ORDER).action(this.validateOrderAction)
                .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATION_PENDING).target(BeerOrderStatusEnum.VALIDATED).event(BeerOrderEventEnum.VALIDATION_PASSED)
                .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATION_PENDING).target(BeerOrderStatusEnum.VALIDATION_EXCEPTION).event(BeerOrderEventEnum.VALIDATION_FAILED).action(this.validationFailureAction)
                .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATION_PENDING).target(BeerOrderStatusEnum.CANCELED).event(BeerOrderEventEnum.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATED).target(BeerOrderStatusEnum.ALLOCATION_PENDING).event(BeerOrderEventEnum.ALLOCATE_ORDER).action(this.allocateOrderAction)
                .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATED).target(BeerOrderStatusEnum.CANCELED).event(BeerOrderEventEnum.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATED).event(BeerOrderEventEnum.ALLOCATION_SUCCESS)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATION_EXCEPTION).event(BeerOrderEventEnum.ALLOCATION_FAILED).action(this.allocationFailureAction)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.PENDING_INVENTORY).event(BeerOrderEventEnum.ALLOCATION_NO_INVENTORY)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.CANCELED).event(BeerOrderEventEnum.CANCEL_ORDER)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATED).target(BeerOrderStatusEnum.PICKED_UP).event(BeerOrderEventEnum.BEER_ORDER_PICKED_UP)
                .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATED).target(BeerOrderStatusEnum.CANCELED).event(BeerOrderEventEnum.CANCEL_ORDER);

    }
}

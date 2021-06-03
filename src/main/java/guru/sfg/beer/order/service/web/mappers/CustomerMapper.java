package guru.sfg.beer.order.service.web.mappers;

import br.com.prcompany.beerevents.model.CustomerDto;
import guru.sfg.beer.order.service.domain.Customer;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
public interface CustomerMapper {

    CustomerDto customerToDto(Customer customer);

    Customer dtoToCustomer(CustomerDto customerDto);
}

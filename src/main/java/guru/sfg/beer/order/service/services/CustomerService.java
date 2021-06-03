package guru.sfg.beer.order.service.services;

import br.com.prcompany.beerevents.model.CustomerDto;
import guru.sfg.beer.order.service.web.model.CustomerDtoPagedList;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerDtoPagedList listCustomers(Pageable pageable);

    CustomerDto findById(UUID customerId);
}

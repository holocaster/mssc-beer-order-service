package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.mappers.CustomerMapper;
import guru.sfg.beer.order.service.web.model.CustomerDtoPagedList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerDtoPagedList listCustomers(Pageable pageable) {
        final Page<Customer> all = this.customerRepository.findAll(pageable);

        return new CustomerDtoPagedList(all.stream()
                .map(customerMapper::customerToDto).collect(Collectors.toList()),
                PageRequest.of(all.getPageable().getPageNumber(),
                        all.getPageable().getPageSize()),
                all.getTotalElements());
    }
}

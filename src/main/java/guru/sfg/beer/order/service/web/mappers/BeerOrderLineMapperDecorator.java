package guru.sfg.beer.order.service.web.mappers;

import br.com.prcompany.beerevents.model.BeerDTO;
import br.com.prcompany.beerevents.model.BeerOrderLineDTO;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {

    private BeerService beerService;
    private BeerOrderLineMapper beerOrderLineMapper;

    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }

    @Autowired
    public void setBeerOrderLineMapper(BeerOrderLineMapper beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }

    @Override
    public BeerOrderLineDTO beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDTO orderLineDto = this.beerOrderLineMapper.beerOrderLineToDto(line);
        final Optional<BeerDTO> beerDtoOptional = this.beerService.getBeerByUpc(orderLineDto.getUpc());

        beerDtoOptional.ifPresent(e -> {
            orderLineDto.setBeerName(e.getBeerName());
            orderLineDto.setBeerStyle(e.getBeerStyle().name());
            orderLineDto.setPrice(e.getPrice());
            orderLineDto.setBeerId(e.getId());
        });

        return orderLineDto;
    }


}

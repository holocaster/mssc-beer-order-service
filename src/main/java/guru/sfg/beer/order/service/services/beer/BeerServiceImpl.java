package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.web.model.BeerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class BeerServiceImpl implements BeerService {

    @Value("${inventory_beer_service_host}")
    private String inventoryBeerServiceHost;

    @Value("${beer_path_v1}")
    private String beerPathV1;

    @Value("${beer_upc_path_v1}")
    private String beerUpcPathV1;

    private RestTemplate restTemplate;

    public BeerServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID uuid) {
        return Optional.of(this.restTemplate.getForObject(this.inventoryBeerServiceHost + this.beerPathV1 + uuid.toString(), BeerDTO.class));
    }

    @Override
    public Optional<BeerDTO> getBeerByUpc(String upc) {
        return Optional.of(this.restTemplate.getForObject(this.inventoryBeerServiceHost + this.beerUpcPathV1 + upc, BeerDTO.class));
    }
}

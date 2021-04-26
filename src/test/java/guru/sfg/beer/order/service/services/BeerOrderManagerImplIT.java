package guru.sfg.beer.order.service.services;

import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.model.BeerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    public static final String FAIL_VALIDATION = "fail-validation";

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    Customer testCustomer;
    UUID beerID = UUID.randomUUID();


    @Value("${beer_upc_path_v1}")
    private String beerUpcPathV1;

    @BeforeEach
    void setUp() {
        this.testCustomer = this.customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    @Test
    void testFailedValidation() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerID).upc("12345").build();

        this.wireMockServer.stubFor(WireMock.get(this.beerUpcPathV1).willReturn(
                WireMock.okJson(this.objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = this.createBeerOrder();
        beerOrder.setCustomerRef(FAIL_VALIDATION);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);


        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            Assertions.assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, foundOrder.getOrderStatus());
        });

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerID).upc("12345").build();

        this.wireMockServer.stubFor(WireMock.get(this.beerUpcPathV1).willReturn(
                WireMock.okJson(this.objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = this.createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            Assertions.assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerID).upc("12345").build();

        this.wireMockServer.stubFor(WireMock.get(this.beerUpcPathV1).willReturn(
                WireMock.okJson(this.objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = this.createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        this.beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            Assertions.assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

        BeerOrder pickedUpOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();

        Assertions.assertNotNull(pickedUpOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedUpOrder.getOrderStatus());
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(this.testCustomer)
                .build();

        Set<BeerOrderLine> beerOrderLines = new HashSet<>();
        beerOrderLines.add(BeerOrderLine.builder().beerId(this.beerID).orderQuantity(1).beerOrder(beerOrder).build());
        beerOrder.setBeerOrderLines(beerOrderLines);

        return beerOrder;
    }

    @TestConfiguration
    static class RestTemplateBuilderProvider {

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }

}

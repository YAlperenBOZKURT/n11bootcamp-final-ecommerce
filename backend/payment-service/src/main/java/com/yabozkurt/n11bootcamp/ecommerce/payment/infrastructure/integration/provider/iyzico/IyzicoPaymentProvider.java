package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.iyzico;

import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.Payment;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.request.CreatePaymentRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProvider;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "iyzico")
public class IyzicoPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(IyzicoPaymentProvider.class);

    private final String apiKey;
    private final String secretKey;
    private final String baseUrl;

    public IyzicoPaymentProvider(@Value("${iyzico.api-key}") String apiKey,
                                 @Value("${iyzico.secret-key}") String secretKey,
                                 @Value("${iyzico.base-url:https://sandbox-api.iyzipay.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return "iyzico";
    }

    @Override
    public PaymentProviderResult pay(PaymentProviderRequest request) {
        try {
            var normalizedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

            Options options = new Options();
            options.setApiKey(apiKey);
            options.setSecretKey(secretKey);
            options.setBaseUrl(baseUrl);

            CreatePaymentRequest iyzicoReq = new CreatePaymentRequest();
            iyzicoReq.setLocale(Locale.TR.getValue());
            iyzicoReq.setConversationId(request.getOrderId());
            iyzicoReq.setPrice(normalizedAmount);
            iyzicoReq.setPaidPrice(normalizedAmount);
            iyzicoReq.setCurrency(Currency.TRY.name());
            iyzicoReq.setInstallment(1);
            iyzicoReq.setBasketId(request.getOrderId());
            iyzicoReq.setPaymentChannel("WEB");
            iyzicoReq.setPaymentGroup("PRODUCT");

            PaymentCard card = new PaymentCard();
            card.setCardHolderName(request.getCardHolderName());
            card.setCardNumber(request.getCardNumber());
            card.setExpireMonth(request.getExpireMonth());
            card.setExpireYear(request.getExpireYear());
            card.setCvc(request.getCvc());
            card.setRegisterCard(0);
            iyzicoReq.setPaymentCard(card);

            Buyer buyer = new Buyer();
            buyer.setId("buyer-" + request.getOrderId());
            buyer.setName(request.getCardHolderName());
            buyer.setSurname("User");
            buyer.setGsmNumber("+905555555555");
            buyer.setEmail("buyer+" + request.getOrderId().toLowerCase() + "@example.com");
            buyer.setIdentityNumber("11111111111");
            buyer.setLastLoginDate("2015-10-05 12:43:35");
            buyer.setRegistrationDate("2013-04-21 15:12:09");
            buyer.setRegistrationAddress("Test Address");
            buyer.setIp("85.34.78.112");
            buyer.setCity("Istanbul");
            buyer.setCountry("Turkey");
            buyer.setZipCode("34732");
            iyzicoReq.setBuyer(buyer);

            Address shippingAddress = new Address();
            shippingAddress.setContactName(request.getCardHolderName());
            shippingAddress.setCity("Istanbul");
            shippingAddress.setCountry("Turkey");
            shippingAddress.setAddress("Test shipping address");
            shippingAddress.setZipCode("34742");
            iyzicoReq.setShippingAddress(shippingAddress);

            Address billingAddress = new Address();
            billingAddress.setContactName(request.getCardHolderName());
            billingAddress.setCity("Istanbul");
            billingAddress.setCountry("Turkey");
            billingAddress.setAddress("Test billing address");
            billingAddress.setZipCode("34742");
            iyzicoReq.setBillingAddress(billingAddress);

            BasketItem basketItem = new BasketItem();
            basketItem.setId("BI-" + UUID.randomUUID().toString().substring(0, 8));
            basketItem.setName("Order " + request.getOrderId());
            basketItem.setCategory1("General");
            basketItem.setItemType(BasketItemType.PHYSICAL.name());
            basketItem.setPrice(normalizedAmount);

            ArrayList<BasketItem> basketItems = new ArrayList<>();
            basketItems.add(basketItem);
            iyzicoReq.setBasketItems(basketItems);

            Payment payment = Payment.create(iyzicoReq, options);
            if ("success".equalsIgnoreCase(payment.getStatus())) {
                return PaymentProviderResult.success(payment.getPaymentId());
            }

            String reason = payment.getErrorMessage() != null && !payment.getErrorMessage().isBlank()
                    ? payment.getErrorMessage()
                    : "Iyzico ödeme başarısız";
            return PaymentProviderResult.fail(reason);
        } catch (Exception ex) {
            log.error("Iyzico checkout exception for order={}: {}", request.getOrderId(), ex.getMessage());
            return PaymentProviderResult.fail("Iyzico checkout hatası: " + ex.getMessage());
        }
    }

    @Override
    public PaymentProviderResult refund(String providerPaymentId) {
        return PaymentProviderResult.fail("Iyzico provider henüz refund detaylarıyla tamamlanmadı");
    }
}

package com.gojek.esb.sink.http.request.uri;

import com.gojek.de.stencil.client.ClassLoadStencilClient;
import com.gojek.de.stencil.client.StencilClient;
import com.gojek.de.stencil.parser.ProtoParser;
import com.gojek.esb.booking.BookingLogMessage;
import com.gojek.esb.consumer.EsbMessage;
import com.gojek.esb.consumer.TestKey;
import com.gojek.esb.consumer.TestMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UriParserTest {
    @Mock
    private ProtoParser protoParser;
    private ProtoParser testMessageProtoParser;
    private ProtoParser bookingMessageProtoParser;
    private EsbMessage testEsbMessage;
    private EsbMessage bookingEsbMessage;

    @Before
    public void setUp() {
        initMocks(this);
        TestKey testKey = TestKey.newBuilder().setOrderNumber("ORDER-1-FROM-KEY").build();
        TestMessage testMessage = TestMessage.newBuilder().setOrderNumber("test-order").setOrderDetails("ORDER-DETAILS").build();
        BookingLogMessage bookingMessage = BookingLogMessage.newBuilder().setOrderNumber("bookingOrderNumber").setCustomerTotalFareWithoutSurge(2000L).setAmountPaidByCash(12.3F).build();

        testEsbMessage = new EsbMessage(testKey.toByteArray(), testMessage.toByteArray(), "test", 1, 11);
        bookingEsbMessage = new EsbMessage(testKey.toByteArray(), bookingMessage.toByteArray(), "test", 1, 11);

        StencilClient stencilClient = new ClassLoadStencilClient();
        testMessageProtoParser = new ProtoParser(stencilClient, TestMessage.class.getCanonicalName());
        bookingMessageProtoParser = new ProtoParser(stencilClient, BookingLogMessage.class.getCanonicalName());
    }

    @Test
    public void shouldReturnTheServiceUrlAsItIsWhenServiceUrlIsNotParametrizedAndParserModeIsMessage() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = "http://dummyurl.com";
        String parsedUrl = uriParser.parse(testEsbMessage, serviceUrl);

        assertEquals(serviceUrl, parsedUrl);
    }

    @Test
    public void shouldReturnTheServiceUrlAsItIsWhenServiceUrlIsNotParametrizedAndParserModeIsKey() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "key");
        String serviceUrl = "http://dummyurl.com";
        String parsedUrl = uriParser.parse(testEsbMessage, serviceUrl);

        assertEquals(serviceUrl, parsedUrl);
    }

    @Test
    public void shouldSetTheValueInServiceUrlFromGivenProtoIndexWhenServiceUrlIsParametrizedAndParserModeIsMessage() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = "http://dummyurl.com/%s,1";
        String parsedUrl = uriParser.parse(testEsbMessage, serviceUrl);

        assertEquals("http://dummyurl.com/test-order", parsedUrl);
    }

    @Test
    public void shouldSetTheValueInServiceUrlFromGivenProtoIndexWhenServiceUrlIsParametrizedAndParserModeIsKey() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "key");
        String serviceUrl = "http://dummyurl.com/%s,1";
        String parsedUrl = uriParser.parse(testEsbMessage, serviceUrl);

        assertEquals("http://dummyurl.com/ORDER-1-FROM-KEY", parsedUrl);
    }

    @Test
    public void shouldSetTheFloatValueInServiceUrlFromGivenProtoIndexWhenServiceUrlIsParametrizedAndParserModeIsMessage() {
        UriParser uriParser = new UriParser(bookingMessageProtoParser, "message");
        String serviceUrl = "http://dummyurl.com/%.2f,16";
        String parsedUrl = uriParser.parse(bookingEsbMessage, serviceUrl);

        assertEquals("http://dummyurl.com/12.30", parsedUrl);
    }

    @Test
    public void shouldSetTheLongValueInServiceUrlFromGivenProtoIndexWhenServiceUrlIsParametrizedAndParserModeIsMessage() {
        UriParser uriParser = new UriParser(bookingMessageProtoParser, "message");
        String serviceUrl = "http://dummyurl.com/%d,52";
        String parsedUrl = uriParser.parse(bookingEsbMessage, serviceUrl);

        assertEquals("http://dummyurl.com/2000", parsedUrl);
    }

    @Test
    public void shouldCatchInvalidProtocolBufferExceptionFromProtoParserAndThrowIllegalArgumentException() throws InvalidProtocolBufferException {
        when(protoParser.parse(any())).thenThrow(new InvalidProtocolBufferException(""));
        UriParser uriParser = new UriParser(protoParser, "message");
        String serviceUrl = "http://dummyurl.com/%s,1";

        try {
            uriParser.parse(testEsbMessage, serviceUrl);
        } catch (IllegalArgumentException e) {
            assertEquals("Unable to parse Service URL", e.getMessage());
        }

    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmptyServiceUrlIsProvided() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = "";
        try {
            uriParser.parse(testEsbMessage, serviceUrl);
        } catch (IllegalArgumentException e) {
            assertEquals("Service URL '" + serviceUrl + "' is invalid", e.getMessage());
        }

    }

    @Test
    public void shouldThrowInvalidConfigurationExceptionWhenNoUrlAndArgumentsAreProvided() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = ",,,";
        try {
            uriParser.parse(testEsbMessage, serviceUrl);
        } catch (InvalidConfigurationException e) {
            assertEquals("Empty Service URL configuration: '" + serviceUrl + "'", e.getMessage());
        }

    }

    @Test
    public void shouldCatchNumberFormatExceptionAndThrowIllegalArgumentsException() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = "http://dummy.com/%s, 6a";
        try {
            uriParser.parse(testEsbMessage, serviceUrl);
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid Proto Index", e.getMessage());
        }

    }

    @Test
    public void shouldThrowIllegalArgumentsExceptionWhenDescriptorIsNotFoundForTheProtoIndexProvided() {
        UriParser uriParser = new UriParser(testMessageProtoParser, "message");
        String serviceUrl = "http://dummy.com/%s, 1000";
        try {
            uriParser.parse(testEsbMessage, serviceUrl);
        } catch (IllegalArgumentException e) {
            assertEquals("Descriptor not found for index: 1000", e.getMessage());
        }

    }

}
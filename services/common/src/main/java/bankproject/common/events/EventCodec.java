package bankproject.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** JSON (de)serialization for {@link BankEvent}s, independent of the web layer's ObjectMapper. */
public final class EventCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static String encode(BankEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize " + event, e);
        }
    }

    public static BankEvent decode(String json) {
        try {
            return MAPPER.readValue(json, BankEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize event: " + json, e);
        }
    }

    private EventCodec() {
    }
}

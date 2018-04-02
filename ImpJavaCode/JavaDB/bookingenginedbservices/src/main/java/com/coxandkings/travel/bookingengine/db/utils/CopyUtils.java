package com.coxandkings.travel.bookingengine.db.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;

public class CopyUtils {
    //TODO: ObjectMapper.convertValue(...) is an expensive operation needs to identify best way for this
    public static <T> T copy(Object resource, Class<T> claszz) throws InvocationTargetException, IllegalAccessException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.convertValue(resource, claszz);
    }
}
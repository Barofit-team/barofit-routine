package com.barofit.routine.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** 횟수·시간·세트 수를 소수나 문자열에서 암묵적으로 변환하지 않는다. */
public class StrictIntegerDeserializer extends ValueDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Integer.class, "정수 값만 입력할 수 있습니다.");
        }
        return parser.getIntValue();
    }
}

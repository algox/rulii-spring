package org.rulii.spring.test.model;

import org.rulii.convert.ConversionException;
import org.rulii.convert.ConverterTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Type;

@Component
public class PersonConverter extends ConverterTemplate<String, Person> {

    public PersonConverter() {
        super();
    }

    @Override
    public Person convert(String value, Type toType) throws ConversionException {
        Assert.notNull(value, "value cannot be null.");
        String[] values = value.split(",");
        if (values.length != 3) return null;
        return new Person(values[0], values[1], Integer.parseInt(values[2]));
    }
}

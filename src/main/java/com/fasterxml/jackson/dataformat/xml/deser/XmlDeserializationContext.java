package com.fasterxml.jackson.dataformat.xml.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.TokenStreamFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;

/**
 * XML-specific {@link DeserializationContext} needed to override certain
 * handlers.
 *
 * @since 2.12
 */
public class XmlDeserializationContext
    extends DefaultDeserializationContext
{
    public XmlDeserializationContext(TokenStreamFactory tsf,
            DeserializerFactory deserializerFactory, DeserializerCache cache,
            DeserializationConfig config, FormatSchema schema,
            InjectableValues values) {
        super(tsf, deserializerFactory, cache,
                config, schema, values);
    }

    /*
    /**********************************************************
    /* Overrides we need
    /**********************************************************
     */

    // [dataformat-xml#374]: need to remove handling of expected root name unwrapping
    // to match serialization
    @Override
    public Object readRootValue(JsonParser p, JavaType valueType,
            JsonDeserializer<Object> deser, Object valueToUpdate)
        throws IOException
    {
//        if (_config.useRootWrapping()) {
//            return _unwrapAndDeserialize(p, valueType, deser, valueToUpdate);
//        }
        if (valueToUpdate == null) {
            return deser.deserialize(p, this);
        }
        return deser.deserialize(p, this, valueToUpdate);
    }

    // To support case where XML element has attributes as well as CDATA, need
    // to "extract" scalar value (CDATA), after the fact
    @Override
    public String extractScalarFromObject(JsonParser p, JsonDeserializer<?> deser,
            Class<?> scalarType)
        throws IOException
    {
        // Only called on START_OBJECT, should not need to check, but JsonParser we
        // get may or may not be `FromXmlParser` so traverse using regular means
        String text = "";

        while (p.nextToken() == JsonToken.FIELD_NAME) {
            // Couple of ways to find "real" textual content. One is to look for
            // "XmlText"... but for that would need to know configuration. Alternatively
            // could hold on to last text seen -- but this might be last attribute, for
            // empty element. So for now let's simply hard-code check for empty String
            // as marker and hope for best
            final String propName = p.currentName();
            JsonToken t = p.nextToken();
            if (t == JsonToken.VALUE_STRING) {
                if (propName.equals("")) {
                    text = p.getText();
                }
            } else {
                p.skipChildren();
            }
        }
        return text;
    }
}

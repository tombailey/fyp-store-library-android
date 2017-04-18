package me.tombailey.store.http.form.body;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URLEncoder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tomba on 25/02/2017.
 */

@RunWith(MockitoJUnitRunner.class)
public class UrlEncodedFormTest {

    @Test
    public void whenContentTypeIsRetrieved_shouldProduceFormUrlEncoded() {
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder().build();
        assertThat(encodedForm.getContentType(),
                is("application/x-www-form-urlencoded; charset=UTF-8"));
    }

    @Test
    public void whenCreateBodyWithEscapableName_shouldEscapeName() throws IOException {
        //arrange
        String paramName = "na me";
        String paramValue = "value";
        byte[] expected = (URLEncoder.encode(paramName, "UTF-8") + "=" + paramValue)
                .getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(paramName, paramValue)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

    @Test
    public void whenCreateBodyWithEscapableValue_shouldEscapeValue() throws IOException {
        //arrange
        String paramName = "name";
        String paramValue = "val ue";
        byte[] expected = (paramName + "=" + URLEncoder.encode(paramValue, "UTF-8"))
                .getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(paramName, paramValue)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

    @Test
    public void whenCreateSingleStringParameterBody_shouldProduceSingleParameterForm() throws IOException {
        //arrange
        String paramName = "name";
        String paramValue = "value";
        byte[] expected = (paramName + "=" + paramValue).getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(paramName, paramValue)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

    @Test
    public void whenCreateSingleIntParameterBody_shouldProduceSingleParameterForm() throws IOException {
        //arrange
        String paramName = "name";
        int paramValue = 42;
        byte[] expected = (paramName + "=" + paramValue).getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(paramName, paramValue)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

    @Test
    public void whenCreateSingleBooleanParameterBody_shouldProduceSingleParameterForm() throws IOException {
        //arrange
        String paramName = "name";
        boolean paramValue = true;
        byte[] expected = (paramName + "=" + paramValue).getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(paramName, paramValue)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

    @Test
    public void whenCreateMultipleParameterBody_shouldProduceMultipleParameterForm() throws IOException {
        //arrange
        String param1Name = "name1";
        String param2Name = "name2";
        String param3Name = "name3";
        String param1Value = "value";
        int param2Value = 42;
        boolean param3Value = true;

        byte[] expected = (param1Name + "=" + param1Value + "&" +
                param2Name + "=" + param2Value + "&" +
                param3Name + "=" + param3Value).getBytes("UTF-8");

        //act
        UrlEncodedForm encodedForm = new UrlEncodedForm.Builder()
                .add(param1Name, param1Value)
                .add(param2Name, param2Value)
                .add(param3Name, param3Value)
                .build();

        //assert
        assertThat(encodedForm.getBytes(), is(expected));
    }

}

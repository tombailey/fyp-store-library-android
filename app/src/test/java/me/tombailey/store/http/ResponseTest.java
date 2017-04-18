package me.tombailey.store.http;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by tomba on 18/04/2017.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(Response.class)
public class ResponseTest {

    @Test
    public void whenGetHttpVersion_shouldGetHttpVersionCorrectly() {
        //arrange
        String expected = "1.1";

        //act
        Response underTest = new Response(expected, 200, "OK", new Header[]{}, new byte[]{});

        //assert
        Assert.assertThat(underTest.getHttpVersion(), Is.is(expected));
    }

    @Test
    public void whenGetStatusCode_shouldGetStatusCodeCorrectly() {
        //arrange
        int expected = 200;

        //act
        Response underTest = new Response("HTTP/1.1", expected, "OK", new Header[]{}, new byte[]{});

        //assert
        Assert.assertThat(underTest.getStatusCode(), Is.is(expected));
    }

    @Test
    public void whenGetStatusText_shouldGetStatusTextCorrectly() {
        //arrange
        String expected = "OK";

        //act
        Response underTest = new Response("HTTP/1.1", 200, expected, new Header[]{}, new byte[]{});

        //assert
        Assert.assertThat(underTest.getStatusText(), Is.is(expected));
    }

   @Test
    public void whenGetHeaders_shouldGetHeadersCorrectly() {
        //arrange
        Header[] expected = new Header[] {
                new Header("host", "example.com")
        };

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", expected, new byte[]{});

        //assert
        Assert.assertThat(underTest.getHeaders(), Is.is(expected));
    }

    @Test
    public void whenGetHeader_shouldGetHeaderCorrectly() {
        //arrange
        Header expected = new Header("host", "example.com");

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", new Header[] {
            expected
        }, new byte[]{});

        //assert
        Assert.assertThat(underTest.getHeader("host"), Is.is(expected));
    }

    @Test
    public void whenGetHeaderThatDoesNotExist_shouldReturnNull() {
        //arrange
        Header expected = null;

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", new Header[] {}, new byte[]{});

        //assert
        Assert.assertThat(underTest.getHeader("host"), Is.is(expected));
    }

    @Test
    public void whenGetMessageBody_shouldGetMessageBodyCorrectly() {
        //arrange
        byte[] expected = "hello world".getBytes();

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", new Header[]{}, expected);

        //assert
        Assert.assertThat(underTest.getMessageBody(), Is.is(expected));
    }

    @Test
    public void whenGetMessageBodyAsString_shouldGetMessageBodyCorrectly() throws UnsupportedEncodingException {
        //arrange
        String expected = "hello world";

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", new Header[]{
                new Header("content-type", "plain/text charset=utf-8")
        }, expected.getBytes());

        //assert
        Assert.assertThat(underTest.getMessageBodyString(), Is.is(expected));
    }

    @Test
    public void whenGetLength_shouldGetLengthCorrectly() throws UnsupportedEncodingException {
        //arrange
        int expected = (
                "HTTP/1.1 200 OK\r\n" +
                "host: example.com\r\n" +
                "\r\n" +
                "hello world"
            ).getBytes().length;

        //act
        Response underTest = new Response("HTTP/1.1", 200, "OK", new Header[]{
            new Header("host", "example.com")
        }, "hello world".getBytes());

        //assert
        Assert.assertThat(underTest.length(), Is.is(expected));
    }

    @Test
    public void whenGetFromInputStream_shouldProduceResponseCorrectly() throws IOException {
        //arrange
        Response expected = new Response("HTTP/1.1", 200, "OK", new Header[]{
                new Header("content-type", "plain/text charset=utf-8")
        }, "hello world".getBytes("utf-8"));

        InputStream mockInputStream = mock(InputStream.class);
        final boolean[] readCalled = {false};
        when(mockInputStream.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                if (!readCalled[0]) {
                    readCalled[0] = true;

                    byte[] buffer = (byte[]) invocation.getArguments()[0];

                    byte[] headerBytes =  ("HTTP/1.1 200 OK\r\n" +
                            "content-type: plain/text charset=utf-8\r\n\r\n").getBytes("ISO-8859-1");
                    byte[] bodyBytes =  ("hello world").getBytes("utf-8");

                    System.arraycopy(headerBytes, 0, buffer, 0, headerBytes.length);
                    System.arraycopy(bodyBytes, 0, buffer, headerBytes.length, bodyBytes.length);

                    return headerBytes.length + bodyBytes.length;
                } else {
                    return 0;
                }
            }
        });

        //act
        Response actual = Response.fromInputStream(mockInputStream);

        //assert
        Assert.assertThat(actual.getHttpVersion(), Is.is(expected.getHttpVersion()));
        Assert.assertThat(actual.getStatusCode(), Is.is(expected.getStatusCode()));
        Assert.assertThat(actual.getStatusText(), Is.is(expected.getStatusText()));
        Assert.assertThat(actual.getMessageBody(), Is.is(expected.getMessageBody()));
    }

}

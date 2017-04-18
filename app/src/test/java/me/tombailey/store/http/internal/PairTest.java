package me.tombailey.store.http.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * Created by tomba on 25/02/2017.
 */

@RunWith(MockitoJUnitRunner.class)
public class PairTest {

    @Test
    public void whenFirstRetrieved_shouldProduceFirst() {
        //arrange
        String expected = "hello";

        //act
        Pair<String, Boolean> underTest = new Pair<>(expected, true);

        //assert
        assertThat(underTest.first(), is(expected));
    }

    @Test
    public void whenSecondRetrieved_shouldProduceSecond() {
        //arrange
        boolean expected = true;

        //act
        Pair<String, Boolean> underTest = new Pair<>("hello", expected);

        //assert
        assertThat(underTest.second(), is(expected));
    }

}
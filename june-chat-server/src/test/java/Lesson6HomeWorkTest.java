import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.geekbrains.june.chat.server.Lesson6HomeWork;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Lesson6HomeWorkTest {

    Lesson6HomeWork classInstance;

    private static Stream<Arguments> paramsForArrayTesting() {
        List<Arguments> argumentsList = new ArrayList<>();

        final int testArrayLength = 8;
        int element4place = 0;
        for (int i = 0; i < 8; i++) {
            int[] testArr = new int[testArrayLength];
            int[] resArray = new int[testArrayLength - (element4place + 1)];
            for (int j = 0; j < testArrayLength; j++) {
                if (j == element4place) {
                    testArr[j] = 4;
                    continue;
                }
                int a;
                while ((a = (int) (Math.random() * 10)) != 4) {
                    testArr[j] = a;
                    continue;
                }
            }
            for (int j = 0; j < resArray.length; j++) {
                resArray[j] = testArr[j + 1 + element4place];
            }
            element4place++;
            argumentsList.add(Arguments.arguments(testArr, resArray));
        }
        return argumentsList.stream();
    }

    public static Stream<Arguments> dataForTestingTask2() {
        List<Arguments> out = new ArrayList<>();
        out.add(Arguments.arguments(new int[]{1, 1, 1}, false));
        out.add(Arguments.arguments(new int[]{2, 2, 2}, false));
        out.add(Arguments.arguments(new int[]{1, 1, 4}, true));
        out.add(Arguments.arguments(new int[]{4, 4, 1, 1}, true));
        // test cases from task
        out.add(Arguments.arguments(new int[]{1, 1, 1, 4, 4, 1, 4, 4}, true));
        out.add(Arguments.arguments(new int[]{1, 1, 1, 1, 1, 1}, false));
        out.add(Arguments.arguments(new int[]{4, 4, 4, 4}, false));
        out.add(Arguments.arguments(new int[]{1, 4, 4, 1, 1, 4, 3}, false));
        return out.stream();
    }

    @BeforeEach
    void init() {
        System.out.println("Init method");
        classInstance = new Lesson6HomeWork();
    }

    @Test
    @DisplayName("Testing if Task 1 input Array throws exception")
    void testIfTask1EmptyArraysAreTreatedCorrectly() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> classInstance.elementsAfterLast4(new int[]{})
        );
    }

    @Test
    @DisplayName("Testing if Task 2 input Array throws exception")
    void testIfTask2EmptyArraysAreTreatedCorrectly() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> classInstance.checkIfArrayConsistsOfOnesAndFours(new int[]{})
        );
    }

    @Test
    @DisplayName("Testing threw exception Task 1")
    void testIfTask1ArrayIncludes4() {
        Assertions.assertThrows(
                RuntimeException.class,
                () -> classInstance.elementsAfterLast4(new int[]{1, 2, 3, 6})
        );
    }

    @Test
    @DisplayName("Testing task 1 example test case")
    void exampleTestCase() {
        Assertions.assertArrayEquals(
                new int[]{1, 7},
                classInstance.elementsAfterLast4(new int[]{1, 2, 4, 4, 2, 3, 4, 1, 7})
        );
    }

    @ParameterizedTest
    @MethodSource("paramsForArrayTesting")
    @DisplayName("Advanced Parametrized Array test for Task 1")
    void advancedParTestingTask1(int[] testArr, int[] resArray) {
        Assertions.assertArrayEquals(
                resArray,
                classInstance.elementsAfterLast4(testArr)
        );
    }

    @ParameterizedTest
    @MethodSource("dataForTestingTask2")
    @DisplayName("Advanced Parametrized Array test for Task 2")
    void advancedParTestingTask2(int[] testArr, boolean returnValue) {
        Assertions.assertEquals(
                returnValue,
                classInstance.checkIfArrayConsistsOfOnesAndFours(testArr)
        );
    }

}

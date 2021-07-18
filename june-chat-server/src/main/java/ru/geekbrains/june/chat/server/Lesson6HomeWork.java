package ru.geekbrains.june.chat.server;

public class Lesson6HomeWork {
    public boolean checkIfArrayConsistsOfOnesAndFours(int[] arr) {
        if (arr.length == 0) throw new NullPointerException("Empty Array");

        boolean onesFlag = false;
        boolean foursFlag = false;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 1 && arr[i] != 4) {
                return false;
            }
            if (arr[i] == 1) onesFlag = true;
            if (arr[i] == 4) foursFlag = true;
        }
        return onesFlag && foursFlag;
    }

    public int[] elementsAfterLast4(int[] arr) {
        if (arr.length == 0) throw new NullPointerException("Empty Array");

        int last = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 4) continue;
            last = i;
        }

        if (last == -1) throw new RuntimeException("Array doesn't contain any 4 digits");

        int[] newArr = new int[arr.length - (last + 1)];

        for (int i = 0; i < newArr.length; i++)
            newArr[i] = arr[last + 1 + i];

        return newArr;

    }
}

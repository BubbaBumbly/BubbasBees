package net.bubba.bubbasbees.util;

public class MathUtils {
    public static boolean inRangeInclusive (int beginRange, int endRange, int isItInThatRange){
        return isItInThatRange >= beginRange && isItInThatRange <= endRange;
    }
}

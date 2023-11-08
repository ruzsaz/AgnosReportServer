package hu.agnos.report.server.service.answerProcessor;

import java.util.Comparator;

import hu.agnos.cube.meta.resultDto.ResultElement;

/**
 *
 * @author parisek
 */
public class KaplenMaierSortbyKnownId implements Comparator<ResultElement> {

    private final int index;

    public KaplenMaierSortbyKnownId(int index) {
        System.out.println("KM: " + index);
        this.index = index;
    }

    // TODO: tesztet írni
    @Override
    public int compare(ResultElement a, ResultElement b) {
        int length = a.header().length;

        for (int i = 0; i < length; i++) {
            if (i != index) {
                int c = a.header()[i].knownId().compareTo(b.header()[i].knownId());
                if (c != 0) {
                    return c;
                }
            }
        }
        return a.header()[index].knownId().compareTo(b.header()[index].knownId());
    }

    public boolean isSameWithoutIndex(ResultElement a, ResultElement b) {
        if (a == null || b == null) {
            return false;
        }
        int length = a.header().length;
        for (int i = 0; i < length; i++) {
            if (i != index) {
                int c = a.header()[i].knownId().compareTo(b.header()[i].knownId());
                if (c != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}

import java.time.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {        
        Scanner scanner = new Scanner(System.in);
        List<Period> periods = createPeriodList(scanner);

        Period result = getSumMinusMin(periods); 

        System.out.println(result);
    }

    public static List<Period> createPeriodList(Scanner scanner) {
        List<Period> list = new ArrayList<>();

        list.add(Period.of(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
        list.add(Period.of(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
        list.add(Period.of(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
        list.add(Period.of(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
        list.add(Period.of(scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));

         /*
        list.add(Period.of(1989, 6, 23));
        list.add(Period.of(1991, 5, 22));
        list.add(Period.of(1994, 4, 21));
        list.add(Period.of(1993, 3, 19));
        list.add(Period.of(1988, 2, 18));
          */

        return list;
    }

    /**
     * Calculates days count in a Period unit.
     * Assumes there are no leap years and all months have 30 days
     */
    private static int calculateDayCount(Period p) {
        return p.getYears() * 365 + p.getMonths() * 30 + p.getDays();
    }

    private static Period getSumMinusMin(List<Period> periods) {
        int min = Integer.MAX_VALUE;
        Period minPeriod = periods.get(0);
        Period total = periods.get(0);
        for (Period p : periods) {
            int dayCount = calculateDayCount(p);
            if (dayCount < min) {
                min = dayCount;
                minPeriod = p;
            }
            if (!p.equals(total)) {
                total = total.plus(p);
            }
        }
        return total.minus(minPeriod);
    }
}
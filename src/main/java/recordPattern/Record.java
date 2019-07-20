package recordPattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Comparator;
import java.util.UUID;


@Data
@EqualsAndHashCode
@ToString
public class Record implements Comparable {
    private UUID primaryKey;
    private String name;
    private Integer age;

    // 出力を見やすくするため
    @Override
    public int compareTo(Object o) {
        if (o instanceof Record) {
            // age 昇順, name 昇順
            Comparator<Record> comparator = Comparator.comparing(Record::getAge).thenComparing(Record::getName);
            return comparator.compare(this, (Record) o);
        }
        return 0;
    }

}

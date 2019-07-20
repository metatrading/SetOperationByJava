package recordPattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.UUID;

/**
 * マスター更新用レコード.
 * <pre>
 *     このVOは、マスター更新を分類するための等価性を実装している。
 *     equalsメソッドは、等価性の判定に{@code primaryKey}を含まない。
 * </pre>
 */
@Data
@EqualsAndHashCode(exclude = {"primaryKey"}, of = {"age", "name"})
@NoArgsConstructor
@ToString
public class ModifiedRecord {

    private UUID primaryKey;
    private Integer age;
    private String name;

    public ModifiedRecord(Record record) {
        try {
            BeanUtils.copyProperties(this, record);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ModifiedRecord modifiedRecordA = new ModifiedRecord();
        ModifiedRecord modifiedRecordB = new ModifiedRecord();

        modifiedRecordA.setAge(2);
        modifiedRecordA.setName("a");
        modifiedRecordA.setPrimaryKey(UUID.randomUUID());

        modifiedRecordB.setAge(2);
        modifiedRecordB.setName("a");
        modifiedRecordB.setPrimaryKey(UUID.randomUUID());

        System.out.println(modifiedRecordA.equals(modifiedRecordB));

        modifiedRecordB.setAge(1);
        System.out.println(modifiedRecordA.equals(modifiedRecordB));
    }
}

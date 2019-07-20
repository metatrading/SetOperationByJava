package recordPattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * レコードの集合操作サンプル
 */
public class RecordPatternSample {
    private Logger logger = LoggerFactory.getLogger(RecordPatternSample.class);

    enum ModifiedPattern {
        NEW,
        UPDATE,
        DELETE,
        NO_MODIFIED
    }

    // ソートして出力したいので、Recordに実装したComparableをTreeSetにて有効化する
    // マスタレコード群
    private SortedSet<Record> masterRecords = new TreeSet<>();
    // リクエストレコード群（何らかの方法で送信されたマスタレコード更新データ）
    private SortedSet<Record> requestRecords = new TreeSet<>();

    public RecordPatternSample() {
        // 100個の要素を持つレコードを生成する
        int totalSize = 100;
        for (int i = 0; i < totalSize; i++) {
            // age は1～10の範囲で生成し、
            // name は2文字の英数字の組み合わせを生成する
            // これにより意図的にRecord#equalsの等価条件を満たした要素を複数発生させることを試みる
            masterRecords.add(createRandomRecord());
        }

        int createSize = masterRecords.size();

        // リクエストレコード群は、0～50個はマスタレコード群のUUIDを設定することで、更新用のリクエストデータとする。
        int existsDataCount = createSize / 2;
        List<Record> createdList = masterRecords.stream().collect(Collectors.toList());
        for (int i = 0; i < existsDataCount; i++) {
            Record requestData = createRandomRecord();
            requestData.setPrimaryKey(createdList.get(i).getPrimaryKey());
            requestRecords.add(requestData);
        }
        while (requestRecords.size() < createSize) {
            requestRecords.add(createRandomRecord());
        }
    }

    public static void main(String[] args) {
        new RecordPatternSample().exec();
    }

    private void exec() {
        logger.info("A.size:{},B.size:{}", masterRecords.size(), requestRecords.size());
        // print用の和集合を得る
        Set<Record> aPlusBSet = getPlusSet(masterRecords, requestRecords);
        logger.info("plus.size():{}", aPlusBSet.size());

        // 和集合をベースに、マスターレコード群、リクエストレコード群の等価要素を出力する
        // ここでＡ集合およびＢ集合の両方が出力されたものは、後続で出力する積集合と同じ要素になるはずである
        logger.info("print A , B.");
        aPlusBSet.stream().forEach(e -> {
            Optional<Record> masterRecord = searchSameRecord(masterRecords, e);
            Optional<Record> requestRecord = searchSameRecord(requestRecords, e);

            ModifiedPattern modifiedPattern = getModifiedPattern(masterRecord, requestRecord);

            logger.info("{}\t master:{},\t request:{}",
                    modifiedPattern,
                    toString(masterRecord),
                    toString(requestRecord));
        });

        // リクエストレコード群にのみ存在する集合を得る。つまり、マスタレコード群に存在しない＝新規データを得る。
        Set<Record> newRecordSet = getSubtractSet(requestRecords, masterRecords);
        logger.info("新規データの表示。");
        newRecordSet.forEach(e -> logger.info("new :{}", toString(Optional.of(e))));

        // マスタレコード群にのみ存在する集合を得る。つまり、リクエストレコード群に存在しない＝削除データを得る
        Set<Record> deleteRecordSet = getSubtractSet(masterRecords, requestRecords);
        logger.info("削除データの表示。");
        deleteRecordSet.forEach(e -> logger.info("delete :{}", toString(Optional.of(e))));

        // マスターレコード群とリクエストレコード群の積集合を得る。つまり更新データを得る
        Set<Record> updateRecordSet = getUpdateSet(masterRecords, requestRecords);
        logger.info("更新データの表示。");
        updateRecordSet.forEach(e -> logger.info("update :{}", toString(Optional.of(e))));

        // マスタレコード群とリクエストレコード群の双方に存在し、すべてが完全一致する
        Set<Record> noModifiedSet = getSameSet(masterRecords, requestRecords);
        logger.info("更新なしデータの表示。");
        noModifiedSet.forEach(e -> logger.info("no modified :{}", toString(Optional.of(e))));
    }

    /**
     * マスター更新パターンを得る
     *
     * @param masterRecord  マスターレコード
     * @param requestRecord リクエストレコード
     * @return マスター更新パターン
     */
    private ModifiedPattern getModifiedPattern(Optional<Record> masterRecord, Optional<Record> requestRecord) {

        if (masterRecord.isPresent() && requestRecord.isPresent()) {

            // ModifiedRecordに置き換えることで、
            // PKを除いた同値性の検証を行い、更新有無を確定させる
            ModifiedRecord masterModifiedRecord = new ModifiedRecord(masterRecord.get());
            ModifiedRecord requestModifiedRecord = new ModifiedRecord(requestRecord.get());

            if (masterModifiedRecord.equals(requestModifiedRecord)) {
                return ModifiedPattern.NO_MODIFIED;
            }

            return ModifiedPattern.UPDATE;
        }

        if (!masterRecord.isPresent()) {
            return ModifiedPattern.NEW;
        }

        if (!requestRecord.isPresent()) {
            return ModifiedPattern.DELETE;
        }

        throw new IllegalStateException();
    }

    /**
     * Setから同じPrimaryKeyのレコードを探す
     *
     * @param set 集合
     * @param e   レコード
     * @return Optionalなレコード
     */
    private Optional<Record> searchSameRecord(Set<Record> set, Record e) {
        return set.stream().filter(t -> t.getPrimaryKey().equals(e.getPrimaryKey())).findFirst();
    }

    /**
     * UUIDと年齢と名前の文字列を得る。
     *
     * @param e レコード
     * @return UUIDと年齢と名前をコロンで挟んだ文字列
     */
    private String toString(Optional<Record> e) {
        if (!e.isPresent())
            return "nothing data";
        return e.map(e2 -> String.join(",", e2.getPrimaryKey().toString(), String.valueOf(e2.getAge()), e2.getName())).get();
    }

    /**
     * 積集合を得る
     *
     * @param setA 集合Ａ
     * @param setB 集合Ｂ
     * @return 積集合
     */
    private Set<Record> getUpdateSet(final Set<Record> setA, final Set<Record> setB) {
        SortedSet<Record> recordSetCopyA = new TreeSet<>(setA);
        SortedSet<Record> recordSetCopyB = new TreeSet<>(setB);

        // PrimaryKey が一致する積集合を得る
        Set<UUID> samePrimarySet = recordSetCopyA.stream()
                .filter(e -> recordSetCopyB.stream().anyMatch(b -> e.getPrimaryKey().equals(b.getPrimaryKey())))
                .map(Record::getPrimaryKey)
                .collect(Collectors.toSet());

        // PrimaryKey が一致するが、他項目が一致しない
        Set<ModifiedRecord> modifiedRecordSetA =
                recordSetCopyA.stream()
                        .map(ModifiedRecord::new)
                        .filter(e -> samePrimarySet.contains(e.getPrimaryKey()))
                        .collect(Collectors.toSet());

        Set<ModifiedRecord> modifiedRecordSetB =
                recordSetCopyB.stream()
                        .map(ModifiedRecord::new)
                        .filter(e -> samePrimarySet.contains(e.getPrimaryKey()))
                        .collect(Collectors.toSet());

        modifiedRecordSetA.removeAll(modifiedRecordSetB);

        Set<Record> returnSet = modifiedRecordSetA.stream().map(e -> {
            Record record = new Record();
            try {
                BeanUtils.copyProperties(record, e);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
            return record;
        }).collect(Collectors.toSet());

        return returnSet;
    }

    /**
     * 和集合を得る。
     *
     * @param setA 集合Ａ
     * @param setB 集合Ｂ
     * @return 集合Ａと集合Ｂの和集合
     */
    private Set<Record> getPlusSet(final SortedSet<Record> setA, final SortedSet<Record> setB) {
        SortedSet<Record> recordSetAcopy = new TreeSet<>(setA);
        SortedSet<Record> recordSetBcopy = new TreeSet<>(setB);

        recordSetAcopy.addAll(recordSetBcopy);

        return recordSetAcopy;
    }

    /**
     * 差集合を得る。
     *
     * @param setA 集合Ａ
     * @param setB 集合Ｂ
     * @return setAからsetBを引いた差集合
     */
    private Set<Record> getSubtractSet(SortedSet<Record> setA, SortedSet<Record> setB) {
        SortedSet<Record> recordSetAcopy = new TreeSet<>(setA);
        SortedSet<Record> recordSetBcopy = new TreeSet<>(setB);

        recordSetAcopy.removeAll(recordSetBcopy);

        return recordSetAcopy;
    }

    /**
     * すべての項目が一致する同値集合を得る。
     *
     * @param setA Ａ集合
     * @param setB Ｂ集合
     * @return Ａ集合とＢ集合のすべての項目が一致する同値集合
     */
    private Set<Record> getSameSet(SortedSet<Record> setA, SortedSet<Record> setB) {
        SortedSet<Record> recordSetAcopy = new TreeSet<>(setA);
        SortedSet<Record> recordSetBcopy = new TreeSet<>(setB);

        recordSetAcopy.retainAll(recordSetBcopy);

        return recordSetAcopy;
    }

    /**
     * ランダムな値を持つレコードを生成する。
     * <ul>
     * <li>id:UUID</li>
     * <li>年齢：1～10のいずれか</li>
     * <li>名前：1桁の英数字の組み合わせ</li>
     * </ul>
     *
     * @return ランダムな値を持つレコード
     */
    Record createRandomRecord() {
        Record record = new Record();
        record.setPrimaryKey(UUID.randomUUID());
        record.setAge(RandomUtils.nextInt(1, 5));

        String[] names = new String[]{"ichiro", "jiro", "saburo", "shiro", "goro", "rokuro"};
        List<String> nameList = Arrays.asList(names);
        record.setName(nameList.get(RandomUtils.nextInt(1, nameList.size())));
        return record;
    }
}

package columnPattern.Column;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode
public class RecordKey {
    private UUID uuid;
    private ModifiedPattern modifiedPattern;
}

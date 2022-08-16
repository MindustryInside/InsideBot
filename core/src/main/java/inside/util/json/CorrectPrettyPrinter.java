package inside.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

public class CorrectPrettyPrinter extends DefaultPrettyPrinter {

    public CorrectPrettyPrinter() {
        _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        withSpacesInObjectEntries();
        _objectFieldValueSeparatorWithSpaces = _separators.getObjectFieldValueSeparator() + " ";
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
        return new CorrectPrettyPrinter();
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!_arrayIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw(']');
    }
}

/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.seasr.meandre.components.transform.text;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.meandre.annotations.Component;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.system.components.ext.StreamInitiator;
import org.meandre.core.system.components.ext.StreamTerminator;
import org.seasr.datatypes.core.DataTypeParser;
import org.seasr.datatypes.core.Names;
import org.seasr.datatypes.datamining.table.Column;
import org.seasr.datatypes.datamining.table.ColumnTypes;
import org.seasr.datatypes.datamining.table.MutableTable;
import org.seasr.datatypes.datamining.table.TableFactory;
import org.seasr.meandre.components.abstracts.AbstractStreamingExecutableComponent;

/**
 * @author Lily Dong
 * @author Boris Capitanu
 */

@Component(
        creator = "Lily Dong",
        description = "Converts token count to table.",
        name = "Token Count To Table",
        tags = "#TRANSFORM, token, count, table, convert",
        firingPolicy = FiringPolicy.any,
        rights = Licenses.UofINCSA,
        baseURL = "meandre://seasr.org/components/foundry/",
        dependency = {"protobuf-java-2.2.0.jar"}
)
public class TokenCountToTable extends AbstractStreamingExecutableComponent {

    //------------------------------ INPUTS ------------------------------------------------------

    @ComponentInput(
            name = Names.PORT_TOKEN_COUNTS,
            description = "The token counts" +
                "<br>TYPE: java.util.Map<java.lang.String, java.lang.Integer>" +
                "<br>TYPE: org.seasr.datatypes.BasicDataTypes.IntegersMap"
    )
    protected static final String IN_TOKEN_COUNTS = Names.PORT_TOKEN_COUNTS;

    @ComponentInput(
            name = Names.PROP_TABLE_FACTORY,
            description = "The TableFactory object" +
                "<br>TYPE: org.seasr.datatypes.table.TableFactory"
    )
    protected static final String IN_TABLE_FACTORY = Names.PROP_TABLE_FACTORY;

    //------------------------------ OUTPUTS -----------------------------------------------------

    @ComponentOutput(
            name = Names.PROP_TABLE,
            description = "Output Table object." +
                "<br>TYPE: org.seasr.datatypes.table.MutableTable"
    )
    protected static final String OUT_TABLE = Names.PROP_TABLE;

    //--------------------------------------------------------------------------------------------


    private boolean _isStreaming;
    private TableFactory _fact;
    private MutableTable _table;
    private Map<String, Integer> _tokenColumnMap;


    //--------------------------------------------------------------------------------------------

    @Override
    public void initializeCallBack(ComponentContextProperties ccp) throws Exception {
        super.initializeCallBack(ccp);

        _isStreaming = false;
        _fact = null;
        _table = null;
        _tokenColumnMap = new HashMap<String, Integer>();
    }

    @Override
    public void executeCallBack(ComponentContext cc) throws Exception {
        if (cc.isInputAvailable(IN_TABLE_FACTORY)) {
            _fact = (TableFactory) cc.getDataComponentFromInput(IN_TABLE_FACTORY);
            _table = (MutableTable) _fact.createTable();
        }

        componentInputCache.storeIfAvailable(cc, IN_TOKEN_COUNTS);

        if (_table == null || !componentInputCache.hasData(IN_TOKEN_COUNTS))
            // Nothing to do yet
            return;

        Object input;
        while ((input = componentInputCache.retrieveNext(IN_TOKEN_COUNTS)) != null) {
            if (input instanceof StreamInitiator) {
                StreamInitiator si = (StreamInitiator) input;
                if (si.getStreamId() != streamId) {
                    // Forward the stream delimiter along
                    cc.pushDataComponentToOutput(OUT_TABLE, si);
                    continue;
                }

                if (_isStreaming)
                    throw new Exception("Stream error - start stream marker already received!");

                _tokenColumnMap.clear();
                _isStreaming = true;

                continue;
            }

            if (input instanceof StreamTerminator) {
                StreamTerminator st = (StreamTerminator) input;
                if (st.getStreamId() != streamId) {
                    // Forward the stream delimiter along
                    cc.pushDataComponentToOutput(OUT_TABLE, st);
                    continue;
                }

                if (!_isStreaming)
                    throw new Exception("Stream error - received end stream marker without start stream!");

                console.fine(String.format("The resulting table has %,d row(s) and %,d column(s)", _table.getNumRows(), _table.getNumColumns()));

                cc.pushDataComponentToOutput(OUT_TABLE, new StreamInitiator(streamId));
                cc.pushDataComponentToOutput(OUT_TABLE, _table);
                cc.pushDataComponentToOutput(OUT_TABLE, new StreamTerminator(streamId));

                _tokenColumnMap.clear();
                _isStreaming = false;

                continue;
            }

            Map<String, Integer> tokenCountsMap = DataTypeParser.parseAsStringIntegerMap(input);
            if (tokenCountsMap.size() == 0) continue;

            int row = _table.getNumRows();
            _table.addRows(1);

            for (Entry<String, Integer> entry : tokenCountsMap.entrySet()) {
                String token = entry.getKey();
                int count = entry.getValue();

                int col;

                if (!_tokenColumnMap.containsKey(token)) {
                    col = _table.getNumColumns();
                    Column column = _fact.createColumn(ColumnTypes.INTEGER);
                    column.setLabel(token);
                    _table.addColumn(column);
                    _tokenColumnMap.put(token, col);
                } else
                    col = _tokenColumnMap.get(token);

                _table.setInt(count, row, col);
            }

            if (!_isStreaming) {
                cc.pushDataComponentToOutput(OUT_TABLE, _table);
                _tokenColumnMap.clear();
            }
        }

    }

    @Override
    public void disposeCallBack(ComponentContextProperties ccp) throws Exception {
        _tokenColumnMap.clear();
        _tokenColumnMap = null;
        _fact = null;
        _table = null;
    }

    //--------------------------------------------------------------------------------------------

    @Override
    public boolean isAccumulator() {
        return true;
    }

    @Override
    public void handleStreamInitiators() throws Exception {
        executeCallBack(componentContext);
    }

    @Override
    public void handleStreamTerminators() throws Exception {
        executeCallBack(componentContext);
    }
}

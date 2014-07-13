<x><![CDATA[

  function mediate(msg) {

     var payload = new XML(msg.getPayloadXML());

     var newPayload = <ns:getQuoteResponse xmlns:ns="http://services.samples/xsd">
                         <ns:return>
                            <ns:last> { payload..Result.toString() } </ns:last>
                         </ns:return>
                      </ns:getQuoteResponse>;

     msg.setPayloadXML(newPayload);
  }


]]></x>



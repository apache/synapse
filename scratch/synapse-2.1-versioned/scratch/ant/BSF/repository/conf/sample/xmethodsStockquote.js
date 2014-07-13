<x><![CDATA[

  function mediateIn(msg) {

     var payload = new XML(msg.getPayloadXML());
     var newPayload = <ns1:getQuote xmlns:ns1="urn:xmethods-delayed-quotes" xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance" xmlns:xsd="http://www.w3.org/1999/XMLSchema">
                         <symbol xsi:type="xsd:string"> { payload..*::symbol.toString() } </symbol>
                      </ns1:getQuote>;
     msg.setPayloadXML(newPayload);
  }


  function mediateOut(msg) {

     var payload = new XML(msg.getPayloadXML());

     var newPayload = <ns:getQuoteResponse xmlns:ns="http://services.samples/xsd">
                         <ns:return>
                            <ns:last> { payload..Result.toString() } </ns:last>
                         </ns:return>
                      </ns:getQuoteResponse>;

     msg.setPayloadXML(newPayload);
  }

]]></x>



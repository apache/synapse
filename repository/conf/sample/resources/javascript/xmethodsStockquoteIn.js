<x><![CDATA[

  function mediate(msg) {

     var payload = new XML(msg.getPayloadXML());
     var newPayload = <ns1:getQuote xmlns:ns1="urn:xmethods-delayed-quotes" xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance" xmlns:xsd="http://www.w3.org/1999/XMLSchema">
                         <symbol xsi:type="xsd:string"> { payload..*::symbol.toString() } </symbol>
                      </ns1:getQuote>;
     msg.setPayloadXML(newPayload);
  }

]]></x>



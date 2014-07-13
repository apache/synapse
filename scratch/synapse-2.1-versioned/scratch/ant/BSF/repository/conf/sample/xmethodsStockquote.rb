<x><![CDATA[

require 'rexml/document'
include REXML

def mediateIn(msg) 
   newRequest = Document.new($REQUEST)
   newRequest.root.elements[1].text = msg.getPayloadXML().root.elements[1].elements[1].get_text
   msg.setPayloadXML(newRequest)
end

def mediateOut(msg) 
   newResponse = Document.new($RESPONSE)
   newResponse.root.elements[1].elements[1].text = msg.getPayloadXML().root.elements[1].get_text
   msg.setPayloadXML(newResponse)
end


$REQUEST = <<EOF
   <ns1:getQuote xmlns:ns1="urn:xmethods-delayed-quotes" xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance" xmlns:xsd="http://www.w3.org/1999/XMLSchema">
      <symbol xsi:type="xsd:string"></symbol>
   </ns1:getQuote>
EOF

$RESPONSE = <<EOF
   <ns:getQuoteResponse xmlns:ns="http://services.samples/xsd">
      <ns:return>
         <ns:last></ns:last>
      </ns:return>
   </ns:getQuoteResponse>
EOF

]]></x>



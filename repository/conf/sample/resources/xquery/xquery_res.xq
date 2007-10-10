<x><![CDATA[
  declare namespace m0="http://services.samples/xsd";
  declare variable $payload as document-node() external;
  <m:CheckPriceResponse xmlns:m="http://www.apache-synapse.org/test">
  	<m:Code>{$payload//m0:return/m0:symbol/child::text()}</m:Code>
  	<m:Price>{$payload//m0:return/m0:last/child::text()}</m:Price>
  </m:CheckPriceResponse>      
]]></x>
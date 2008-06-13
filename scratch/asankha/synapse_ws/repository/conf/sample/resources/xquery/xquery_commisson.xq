<x><![CDATA[
  declare namespace m0="http://services.samples/xsd";
  declare variable $payload as document-node() external;
  declare variable $commission as document-node() external;
  <m0:return xmlns:m0="http://services.samples/xsd">
  	<m0:symbol>{$payload//m0:return/m0:symbol/child::text()}</m0:symbol>
  	<m0:last>{$payload//m0:return/m0:last/child::text()+ $commission//commission/vendor[@symbol=$payload//m0:return/m0:symbol/child::text()]}</m0:last>
  </m0:return>  
]]></x>
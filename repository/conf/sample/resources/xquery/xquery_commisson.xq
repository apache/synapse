<x><![CDATA[
  declare namespace ns="http://services.samples";
  declare variable $payload as document-node() external;
  declare variable $commission as document-node() external;
  <m0:return xmlns:m0="http://services.samples">
  	<m0:symbol>{$payload//ns:return/ns:symbol/child::text()}</m0:symbol>
  	<m0:last>{$payload//ns:return/ns:last/child::text()+ $commission//commission/vendor[@symbol=$payload//ns:return/ns:symbol/child::text()]}</m0:last>
  </m0:return>  
]]></x>
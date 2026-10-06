# Tosca test plan (to be built in Tricentis Tosca)
- API TestCases (Tosca API Scan on /v3 or Postman import): POST /api/claims, PUT /api/claims/{id}/status, GET /api/claims/{id}
- UI TestCases (Tosca XScan on the React app): submit claim -> appears in tracker; decode policy -> summary shown
- Negative: blank fields rejected (400), unknown status rejected (400)

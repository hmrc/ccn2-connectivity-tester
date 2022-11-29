
# CCN2 connectivity tester

This service sends requests, on a configurable schedule, to [api-platform-outbound-soap](https://github.com/hmrc/api-platform-outbound-soap).
Upon receiving these requests api-platform-outbound-soap makes a SOAP request to the EU's CCN2 server, requesting a CoD.
Since these CoD (confirmation of delivery) messages are sent asynchronously, the receipt of one proves the entire round
trip from HMRC to CCN2 and in the other direction. 

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
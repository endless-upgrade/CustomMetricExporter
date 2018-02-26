package it.reply.data.pasquali.controller

import it.reply.data.pasquali.engine.{AnsibleConnector, ClouderaConnector}
import it.reply.data.pasquali.metrics.RecMetricsCollector
import it.reply.data.pasquali.metrics.model.Metric
import org.scalatra.scalate.ScalateSupport
import org.scalatra.{FlashMapSupport, ScalatraServlet}
import org.slf4j.LoggerFactory


class Controller extends ScalatraServlet with FlashMapSupport with ScalateSupport{

  var cloudera : ClouderaConnector = null
  var ansible : AnsibleConnector = null

  val logger = LoggerFactory.getLogger(getClass)

  def initConnectors() : Unit = {

    logger.info(" ******************* Init Ansible Connector")
    ansible = AnsibleConnector(
      "",
      "",
      ""
    )

    logger.info(" ******************* ")
  }

  get("/") {
    if(cloudera == null)
      initConnectors()

    logger.info(" ******************* HOME")

    "This webapp only collect and provides metrics for DevOps echosystem\n"+
    "The collection process is periodically run in order to provide fresh values\n\n"+
    "Use /metrics to get actual metrics\n"+
    "Use /metrics/fresh to get refreshed metrics"


  }

  // *********************** MACHINE STATUS *******************

  // *********************** DATABASE STATE *******************

  // *********************** SERVICE STATUS ********************

  get("/service/status") {

    logger.info(" ******************* SERVICE STATUS")

    var body = ""
    body += "\n"
    body += "/service/status/localhost/jenkins\n"
    body += "/service/status/localhost/prometheus\n"
    body += "/service/status/localhost/node_exporter\n"
    body += "/service/status/localhost/grafana-server\n"
    body += "/service/status/localhost/pushgateway\n"

    val stats = collectMultipleServicesStatus("localhost",
      Array("jenkins", "prometheus", "node_exporter", "grafana-server", "pushgateway"))

    var rr = ""

    rr += s"jenkins on localhost state : ${if(stats(0)) "running" else "stop :("}"
    rr += s"prometheus on localhost state : ${if(stats(1)) "running" else "stop :("}"
    rr += s"node_exporter on localhost state : ${if(stats(2)) "running" else "stop :("}"
    rr += s"grafana on localhost state : ${if(stats(3)) "running" else "stop :("}"
    rr += s"pushgateway on localhost state : ${if(stats(4)) "running" else "stop :("}"

    rr
  }

  get("/service/status/:machine/:service") {
    val vm = params.getOrElse("machine", "0.0.0.0")
    val service = params.getOrElse("service", "")
    val running = collectServiceStatus(vm, service)

    s"$service on $vm state : ${if(running) "running" else "stop :("}"
  }

  // ********************** EXPOSE METRICS *************************

  get("/metrics") {
    if(cloudera == null)
      initConnectors()

    logger.info(" ******************* GET METRICS")

    val stats = collectMultipleServicesStatus("localhost",
      Array("jenkins", "prometheus", "node_exporter", "grafana-server", "pushgateway"))
    RecMetricsCollector.getPrometheusMetrics()
  }

  // ********************** COLLECTORS *****************************
//
//  def collectMachineStatus(vm : String) : Boolean = {
//    val online = ansible.pingMachinePy(vm)
//
//    RecMetricsCollector.addMetric(
//      s"is_online_${vm}",
//      new Metric(s"is_online_${vm}", s"1 if $vm is online, 0 otherwise",
//        if(online) 1 else 0, "devops_exporter", ""))
//
//    logger.info(s" ******************* Metrics update, now ${RecMetricsCollector.metrics.size}")
//    online
//  }
//
//  def collectCountsHive(db : String, table : String) : Long = {
//
//    val count = cloudera.countHive(db, table)
//
//    RecMetricsCollector.addMetric(
//      s"hive_${table}_number",
//      new Metric(s"hive_${table}_number", s"number of ${table} in the ${db}",
//        count, "devops_exporter", ""))
//
//    logger.info(s" ******************* Metrics update, now ${RecMetricsCollector.metrics.size}")
//    count
//  }
//
//  def collectCountsKudu(db : String, table : String) : Long = {
//
//    val count = cloudera.countKudu(db, table)
//
//    RecMetricsCollector.addMetric(
//      s"kudu_${table}_number",
//      new Metric(s"kudu_${table}_number", s"number of ${table} in the ${db}",
//        count, "devops_exporter", ""))
//
//    logger.info(s" ******************* Metrics update, now ${RecMetricsCollector.metrics.size}")
//    count
//  }

  def collectServiceStatus(vm: String, service: String) : Boolean = {
    collectMultipleServicesStatus(vm, Array(service))(0)
  }

  def collectMultipleServicesStatus(vm: String, service: Array[String]) : Array[Boolean] = {

    val running = ansible.checkMultipleServiceRunningSH(vm, service)

    for{
      (serv, i) <- service.zipWithIndex
      (state, j) <- running.zipWithIndex
      if i == j
    }{
      RecMetricsCollector.addMetric(
        s"service_status_$serv",
        new Metric(s"is_online_${vm}_$serv", s"1 if $vm.$serv is running, 0 otherwise",
          if(state) 1 else 0, "devops_exporter", vm))
    }

    logger.info(s" ******************* Metrics update, now ${RecMetricsCollector.metrics.size}")
    running.toArray
  }
}


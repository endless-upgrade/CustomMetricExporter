package it.reply.data.pasquali.engine

import it.reply.data.pasquali.model.AnsibleResult
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.sys.process._

case class AnsibleConnector(ansibleHome : String,
                            SSHKeyFile : String,
                            ansibleSSHUser : String) {


  val logger = LoggerFactory.getLogger(getClass)

  def pingMachine(machineAddress : String) : Boolean = {

    logger.info(" .......................... ANSIBLE PING")

    var res =
      s"""ansible -i '$machineAddress,' all """ +
        s"""--private-key=$SSHKeyFile """ +
        s"""-e 'ansible_ssh_user=$ansibleSSHUser' """ +
        s"""-e 'host_key_checking=False' """ +
        s"""-m ping""" !!

    res.contains(machineAddress) && res.contains("SUCCESS") && res.contains(""""ping": "pong"""")
  }

  def pingMultiple(addrs : Array[String]) : mutable.ArrayBuffer[Boolean] = {

    logger.info(" .......................... ANSIBLE PING")

    var targets = ""
    for(a <- addrs) targets += a+","

    var res =
      s"""ansible -i '$targets,' all """ +
        s"""--private-key=$SSHKeyFile """ +
        s"""-e 'ansible_ssh_user=$ansibleSSHUser' """ +
        s"""-e 'host_key_checking=False' """ +
        s"""-m ping""" !!

    var state : mutable.ArrayBuffer[Boolean] = new mutable.ArrayBuffer[Boolean]()

    for(a <- addrs){
      val pong = s"""$a | SUCCESS >> {""" +
        """"changed": false,""" +
        """"ping": "pong""""+
        """}"""

      state += res.contains(pong)
    }

    state
  }

  def pingMultipleSH(addrs : Array[String]) : mutable.ArrayBuffer[Boolean] = {

    var targets = ""
    for(a <- addrs) targets += a+","

    logger.info(" .......................... ANSIBLE PING")

    var res = s"""/opt/monitoring/ping.sh $targets""" !!

    logger.info(res)

    var state : mutable.ArrayBuffer[Boolean] = new mutable.ArrayBuffer[Boolean]()

    for(a <- addrs){
      val pong = s"""$a | SUCCESS >> {""" +
        """"changed": false,""" +
        """"ping": "pong""""+
        """}"""

      state += res.contains(pong)
    }

    state
  }

  def checkServiceRunning(machineAddress : String, service : String) : Boolean = {

    logger.info(" .......................... ANSIBLE CHECK SERVICE")

    var res = s"""ansible-playbook -i '$machineAddress,' all """ +
      s"""--private-key=$SSHKeyFile """ +
      s"""./ansible/test-service.yml """+
      s"""-e 'ansible_ssh_user=$ansibleSSHUser' """ +
      s"""-e 'host_key_checking=False' """ +
      s"""--extra-vars "service_pretty=$service service=$service" """ +
      s"""| tail -n 2 """ !!

    val ar = getAnsibleRunResult(res)

    ar.name.equals(machineAddress) &&
      (ar.ok >= 1) &&
      (ar.changed >= 0) &&
      (ar.unreachable == 0) &&
      (ar.failed == 0)
  }

  def checkMultipleServiceRunningSH(machineAddress : String, services : Array[String]) : mutable.ArrayBuffer[Boolean] = {

    val targets = services.mkString(",")

    logger.info(" .......................... ANSIBLE CHECK SERVICE")

    var res = s"""/opt/monitoring/test-service.sh $machineAddress $targets""" !!

    logger.info(res)

    var status = new mutable.ArrayBuffer[Boolean]()

    for(s <- services){
      val check = s"ok: [$machineAddress] => (item=$s)"
      status += res.contains(check)
    }

    status
  }

  def getAnsibleRunResult(lastLine : String) : AnsibleResult = {

    val arr = lastLine.split(" ").filter(el => el != "" && el != ":")
    val values = arr.map(e => if(e.contains("=")) e.split("=")(1) else e)
    AnsibleResult(values(0), values(1).toInt, values(2).toInt, values(3).toInt, values(4).toInt)
  }



}

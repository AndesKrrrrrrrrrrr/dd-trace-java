package com.datadog.iast.test

import com.datadog.iast.IastRequestContext
import com.datadog.iast.model.Vulnerability
import com.datadog.iast.taint.TaintedObjects
import datadog.trace.agent.test.base.WithHttpServer
import datadog.trace.agent.tooling.bytebuddy.iast.TaintableVisitor
import datadog.trace.api.gateway.IGSpanInfo
import datadog.trace.api.gateway.RequestContext
import groovy.transform.CompileStatic

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class IastHttpServerTest<SERVER> extends WithHttpServer<SERVER> implements IastRequestContextPreparationTrait {

  private static final LinkedBlockingQueue<TaintedObjectCollection> TAINTED_OBJECTS = new LinkedBlockingQueue<>()
  private static final List<Vulnerability> VULNERABILITIES = new ArrayList<>()

  @CompileStatic
  void configurePreAgent() {
    injectSysConfig('dd.iast.enabled', 'true')
    super.configurePreAgent()
  }

  protected Closure getRequestEndAction() {
    { RequestContext requestContext, IGSpanInfo igSpanInfo ->
      // request end action
      IastRequestContext iastRequestContext = IastRequestContext.get(requestContext)
      if (iastRequestContext) {
        TaintedObjects taintedObjects = iastRequestContext.getTaintedObjects()
        TAINTED_OBJECTS.offer(new TaintedObjectCollection(taintedObjects))
        // VULNERABILITIES.addAll(iastRequestContext.getVulnerabilityBatch().getVulnerabilities())
      }
    }
  }

  def setupSpec() {
    TaintableVisitor.DEBUG = true
  }

  void setup() {
    iastSystemSetup(requestEndAction)
  }

  void cleanup() {
    iastSystemCleanup()
  }

  protected TaintedObjectCollection getFinReqTaintedObjects() {
    TAINTED_OBJECTS.poll(15, TimeUnit.SECONDS)
  }

  protected List<Vulnerability> getVulnerabilities() {
    VULNERABILITIES
  }

  int version() {
    return 0
  }

  @Override
  String service() {
    return null
  }

  @Override
  String operation() {
    return null
  }
}

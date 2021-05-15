local Project = "websocket-scala-client";

local Dir = Project + "/";

local ver211 = "2.11";
local ver212 = "2.12";
local ver213 = "2.13";
local ver3   = "3.0";

local AbstractPipeline(name) = {
  kind: "pipeline",
  type: "docker",
  name: name
};

local Workspace(name) = {
  workspace: { path: name }
};

local WsPipeline(ver) = AbstractPipeline(Dir + ver) + Workspace(ver);

local Pipeline(ver, build, notify) = WsPipeline(ver) + {
  kind: "pipeline",
  type: "docker",
  name: Dir + ver,
  steps: [ build, notify ]
};

local BuildStep(ver) = {
  name: "build",
  image: "andyglow/sbt:latest",
  when: { "branch": "master" },
  environment: {
    SCALA_VER: ver,
    CODECOV_TOKEN: { from_secret: "codecov_token" },
    DRONE_WORKSPACE_PATH: "/drone/src/" + ver
  }
};

local SbtCleanTest(ver) = BuildStep(ver) + {
  commands: [
    "sbt clean test"
  ]
};

local Coverage(name, ver) = BuildStep(ver) + {
  name: name,
  commands: [
     "sbt clean coverage test",
     "sbt coverageAggregate",
     "wget -O .codecov https://codecov.io/bash",
     "chmod +x .codecov",
     "./.codecov -X gcov -X coveragepy -X xcode -X gcovout"
  ]
};

local NotifyMessage = |||
    {{#success build.status}}
      {{repo.name}}: build {{build.number}} for ver %(ver)s succeeded (spent {{since build.started}}). Good job. {{build.link}}
    {{else}}
      {{repo.name}}: build {{build.number}} for ver %(ver)s failed. Fix please. {{build.link}}
    {{/success}}
|||;

local Notify(name, ver) = {
  name: name,
  image: "plugins/slack",
  when: { status: [ "success", "failure" ] },
  settings: {
    webhook: { from_secret: "slack_webhook_url" },
    channel: "builds",
    username: "drone",
    link_names: true,
    template: NotifyMessage % { ver: ver }
  }
};

[
  Pipeline(ver3   , SbtCleanTest(ver3)   , Notify("slack", ver3)),
  Pipeline(ver213 , SbtCleanTest(ver213) , Notify("slack", ver213)),
  Pipeline(ver212 , SbtCleanTest(ver212) , Notify("slack", ver212)),
  Pipeline(ver211 , SbtCleanTest(ver211) , Notify("slack", ver211)),
  AbstractPipeline("finalize") + Workspace(ver213) + {
    steps: [
      Coverage("scoverage", ver213)
    ],
    depends_on: [
      Dir + ver3,
      Dir + ver213,
      Dir + ver212,
      Dir + ver211
    ]
  },
]
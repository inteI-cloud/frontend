(ns circle.backend.build.inference.test-rails
  (:use midje.sweet)
  (:use circle.backend.build.inference.rails)
  (:require [circle.backend.build.test-utils :as test])
  (:require fs)
  (:require [circle.backend.git :as git]))

(fact "bundler?"
  (let [bundler-repo (test-repo "bundler_1")]
    (bundler? bundler-repo) => true
    (bundler? empty-repo) => false))

(fact "bundler action"
  (let [bundler-repo (test-repo "bundler_1")]
    (map :name (spec bundler-repo)) => (contains ["bundle install"])))

(fact "rspec? works"
  (let [rspec-repo (test-repo "rspec_1")]
    (rspec? rspec-repo) => true
    (rspec? empty-repo) => false))

(fact "rspec? doesn't trigger with no .rb files"
  (let [rspec-repo (test-repo "rspec_empty")]
    (rspec? rspec-repo) => false))

(fact "test-unit? doesn't trigger with no .rb files"
  (let [repo (test-repo "rake_test_empty")]
    (test-unit? repo) => false))

(fact "rspec uses bundler when appropriate"
  (let [bundler-repo (test-repo "rspec_1")]
    (->> (spec bundler-repo) (map :name) (into #{})) => (contains #"bundle exec rspec spec"))
  (let [no-bundler-repo (test-repo "no_bundler_1")]
    (->> (spec no-bundler-repo) (map :name)) => (contains "rspec spec")))

(fact "rspec action"
  (let [repo (test-repo "rspec_1")]
    (->> (spec repo) (map :name) (into #{})) => (contains "bundle exec rspec spec")))

(fact "rspec actions have type :test"
  (let [repo (test-repo "rspec_1")]
    (->> (spec repo) (map :type) (into #{})) => (contains :test)))

(fact "db:create when db.yml action"
  (let [repo (test-repo "database_yml_2")]
    (database-yml? repo) => true
    (->> (spec repo) (map :name) (into #{}))  => (contains "bundle exec rake db:create --trace")))

(fact "inference finds database yaml"
  (let [repo (test-repo "database_yml_1")]
    (database-yml? repo) => false
    (find-database-yml repo) => (fs/join repo "config/database.example.yml")))

(fact "inference finds database yaml using 'default' instead of 'example'"
  (let [repo (test-repo "database_yml_3")]
    (database-yml? repo) => false
    (find-database-yml repo) => (fs/join repo "config/database.yml.default")))

(fact "data-mapper? works"
  (let [repo (test-repo "dm_rails_1")]
    (data-mapper? repo) => true
    (data-mapper? empty-repo) => falsey))

(fact "call rake db:automigrate when using dm-rails"
  (let [repo (test-repo "dm_rails_1")]
    (spec repo)
    (->> (spec repo) (map :name) (into #{})) => (contains #"rake db:automigrate")))

(fact "copy database.example.yml to database.yml action"
  ;; repo with database.example.yml
  (let [example-repo (test-repo "database_yml_1")]
    (->> (spec example-repo) (map :name)) => (and (contains "copy database.yml")
                                                  (contains "rake db:create --trace"))))
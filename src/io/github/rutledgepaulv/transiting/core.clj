(ns io.github.rutledgepaulv.transiting.core
  (:require [cognitect.transit :as transit])
  (:import (clojure.lang TaggedLiteral)
           (com.cognitect.transit DefaultReadHandler ReadHandler WriteHandler)
           (java.time Instant)
           (java.util.regex Pattern)
           (java.util.zip GZIPInputStream GZIPOutputStream)))

(def PatternReadWriteHandler
  (reify
    WriteHandler
    (tag [this value]
      "pattern")
    (rep [this value]
      [(str value) (.flags ^Pattern value)])
    ReadHandler
    (fromRep [this [value flags]]
      (Pattern/compile value flags))))

(def InstantReadWriteHandler
  (reify
    WriteHandler
    (tag [this value]
      "instant")
    (rep [this value]
      (str value))
    ReadHandler
    (fromRep [this value]
      (Instant/parse value))))

(def TaggedLiteralWriteHandler
  (reify
    WriteHandler
    (tag [this value]
      (str (.-tag ^TaggedLiteral value)))
    (rep [this value]
      (.-form ^TaggedLiteral value))))

(def DefaultReadWriteHandler
  (reify
    WriteHandler
    (tag [this value]
      (throw (ex-info "No handler found for type" {:type (type value)})))
    (rep [this value]
      (throw (ex-info "No handler found for type" {:type (type value)})))
    DefaultReadHandler
    (fromRep [this tag rep]
      (tagged-literal (symbol tag) rep))))

(def WriteHandlers
  {Pattern       PatternReadWriteHandler
   Instant       InstantReadWriteHandler
   TaggedLiteral TaggedLiteralWriteHandler
   ; work around edge case in transit-java
   ; where object's supertype is null and
   ; so it encodes it as null rather than
   ; throwing an error
   Object        DefaultReadWriteHandler})

(def ReadHandlers
  {(.tag PatternReadWriteHandler "") PatternReadWriteHandler
   (.tag InstantReadWriteHandler "") InstantReadWriteHandler})

(defn encode
  ([data output-stream]
   (encode data output-stream {}))
  ([data output-stream {:keys [handlers format]
                        :or   {handlers {} format :json}
                        :as   options}]
   (with-open [compressed (GZIPOutputStream. output-stream)]
     (let [options {:handlers        (merge WriteHandlers handlers)
                    :default-handler DefaultReadWriteHandler}
           writer  (transit/writer compressed format options)]
       (transit/write writer data)))))

(defn decode
  ([input-stream]
   (decode input-stream {}))
  ([input-stream {:keys [handlers format]
                  :or   {handlers {} format :json}
                  :as   options}]
   (with-open [compressed (GZIPInputStream. input-stream)]
     (let [options {:handlers        (merge ReadHandlers handlers)
                    :default-handler DefaultReadWriteHandler}
           reader  (transit/reader compressed format options)]
       (transit/read reader)))))
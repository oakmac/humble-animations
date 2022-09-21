(ns humble-animations.animate
  (:require
    [tween-clj.core :as tween]))

(def *redraw-fn (atom nil))

(defn now
  "returns the current time in milliseconds"
  []
  (System/currentTimeMillis))

(def transitions
  {:ease-in (partial tween/ease-in tween/transition-linear)
   :ease-out (partial tween/ease-out tween/transition-linear)
   :ease-in-out (partial tween/ease-in-out tween/transition-linear)

   :ease-in-circ (partial tween/ease-in tween/transition-circ)
   :ease-out-circ (partial tween/ease-out tween/transition-circ)
   :ease-in-out-circ (partial tween/ease-in-out tween/transition-circ)

   :ease-in-expo (partial tween/ease-in tween/transition-expo)
   :ease-out-expo (partial tween/ease-out tween/transition-expo)
   :ease-in-out-expo (partial tween/ease-in-out tween/transition-expo)

   :ease-in-pow (partial tween/ease-in tween/transition-pow)
   :ease-out-pow (partial tween/ease-out tween/transition-pow)
   :ease-in-out-pow (partial tween/ease-in-out tween/transition-pow)

   :ease-in-sine (partial tween/ease-in tween/transition-sine)
   :ease-out-sine (partial tween/ease-out tween/transition-sine)
   :ease-in-out-sine (partial tween/ease-in-out tween/transition-sine)})

;; TODO: good candidate for unit tests
(defn calc-tween-val
  "Calculates a tweening value for a specific time"
  [{:keys [start-val end-val start-time end-time current-time transition]}]
  (let [inverted? (> start-val end-val)
        start-val' (if inverted? end-val start-val)
        end-val' (if inverted? start-val end-val)
        default-transition-fn (:ease-in transitions)
        shorthand-fn (get transitions transition)
        transition-fn (cond
                        shorthand-fn shorthand-fn
                        (fn? transition) transition
                        :else (do
                                ;; TODO: warning here
                                default-transition-fn))
        ;; a p-val is a number between 0 and 1, used for the transition functions
        p-val (tween/range-to-p start-time end-time current-time)
        tween-val (tween/p-to-range start-val' end-val' (transition-fn p-val))]
    (if-not inverted?
      tween-val
      (let [delta-from-start-val (- tween-val start-val')]
        (- start-val delta-from-start-val)))))

;; roughly 60 frames per second
(def animation-tick-rate-ms
  16)

;; FIXME: not working yet
(def animating?
  "Are we currently animating?"
  (atom false))

(def animations-queue
  (atom {}))

;; TODO:
;; - assert(duration-ms >= 10)

(defn tick!
  "When an animation is active this function runs every ~16ms, for 60fps
  NOTE: this is a somewhat recursive function that calls itself via a future"
  []
  (let [queue @animations-queue
        n (now)
        stops (atom [])]
    ;; process the queue
    (doseq [[id animation] queue]
      (let [{:keys [id start-val end-val end-time on-tick]} animation
            ;; calculate the tween value for this animation
            tween-val (calc-tween-val (assoc animation :current-time n))
            ascending? (< start-val end-val)
            time-to-stop? (or
                            ;; we have reached their end-val
                            ; (if ascending?
                            ;   (>= tween-val end-val)
                            ;   (<= tween-val end-val))
                            ;; it is past time
                            (> n end-time))]
        ;; run their provided on-tick function with the val
        (when (fn? on-tick) (on-tick {:time n
                                      :val tween-val}))
        ;; is it time to stop this animation?
        (when time-to-stop?
          (swap! stops conj animation))))

    ;; perform a redraw: hopefully the user has done something useful with their
    ;; on-tick function so we may see the animation!
    (when-let [f @*redraw-fn]
      (f))

    ;; process any stops
    (doseq [{:keys [id on-stop] :as anim} @stops]
      ;; run their on-stop function if they provided one
      (when (fn? on-stop) (on-stop anim))
      ;; remove from the animations queue
      (swap! animations-queue dissoc id))

    ;; queue up the next tick! unless the queue is empty
    (when-not (empty? @animations-queue)
      (Thread/sleep animation-tick-rate-ms)
      (tick!))))

;; -----------------------------------------------------------------------------
;; Public API

(defn set-redraw-fn! [f]
  (reset! *redraw-fn f))

;; TODO: make this vardiadic, accept multiple animations
(defn start-animation!
  [{:keys [start-val end-val duration-ms on-tick transition] :as animation}]

  (assert (fn? @*redraw-fn) "Please set the *redraw-fn before calling start-animation!")

  (let [start-time (now)
        end-time (+ start-time duration-ms)
        id (str (random-uuid))
        animation' (assoc animation :id id
                                    :start-time start-time
                                    :end-time end-time
                                    :start-val (double start-val)
                                    :end-val (double end-val))]
    ;; add this animation to the queue
    (swap! animations-queue assoc id animation')

    ;; start the tick! function if it is not running already
    (when-not @animating?
      (future (tick!)))

    ;; return the id of their new animation
    id))

;; TODO: write this
; (defn stop-animation! []
;   (println "Stop animation")
;   (reset! animating? false))

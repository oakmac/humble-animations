(ns humble-animations.animate
  "Manages a queue of animations and provides a tick! function to implement
  incremental progress as the animation occurs."
  (:require
    [tween-clj.core :as tween]))

(def *request-frame-fn (atom nil))

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

(defn convert-vals-to-doubles
  "ensure that values are doubles for the tweening calculations"
  [v]
  (cond
    (number? v) (double v)
    (vector? v) (mapv double v)
    (map? v) (zipmap (keys v) (map double (vals v)))
    :else 0)) ;; TODO: should we warn here?

;; TODO: good candidate for unit tests
(defn calc-tween-val
  "Calculates a single 'tween value for a specific time"
  [{:keys [start-val end-val start-time end-time current-time transition]}]
  (let [inverted? (> start-val end-val)
        start-val' (if inverted? end-val start-val)
        end-val' (if inverted? start-val end-val)
        default-transition-fn (:ease-in transitions)
        shorthand-fn (get transitions transition)
        transition-fn (cond
                        shorthand-fn shorthand-fn
                        (fn? transition) transition
                        :else default-transition-fn)
        ;; a p-val is a number between 0 and 1, used for the transition functions
        p-val (tween/range-to-p start-time end-time current-time)
        tween-val (tween/p-to-range start-val' end-val' (transition-fn p-val))]
    (if-not inverted?
      tween-val
      (let [delta-from-start-val (- tween-val start-val')]
        (- start-val delta-from-start-val)))))

;; TODO: good candidate for unit tests
(defn calc-tween-vals
  [{:keys [start-val end-val] :as animation}]
  (cond
    (number? start-val) (calc-tween-val animation)
    (sequential? start-val) (map-indexed
                              (fn [idx s-val]
                                (-> animation
                                  (assoc :start-val s-val
                                         :end-val (nth end-val idx))
                                  calc-tween-val))
                              start-val)
    (map? start-val) (zipmap (keys start-val)
                             (map-indexed
                               (fn [idx s-val]
                                 (-> animation
                                   (assoc :start-val s-val
                                          :end-val (nth (vals end-val) idx))
                                   calc-tween-val))
                               (vals start-val)))
    ;; NOTE: this should not happen
    :else nil))

;; FIXME: not working yet
(def animating?
  "Are we currently animating?"
  (atom false))

(def animations-queue
  (atom {}))

(defn first-val
  "returns the first value from either a number, vector, or map of values"
  [v]
  (cond
    (number? v) v
    (sequential? v) (first v)
    (map? v) (-> v vals first)
    :else nil))

(defn tick!
  "This function is called continuously while an animation is active."
  []
  (let [queue @animations-queue
        n (now)
        stops (atom [])]
    ;; process the queue
    (doseq [[id animation] queue]
      (let [{:keys [id start-val end-val end-time on-tick]} animation
            ;; calculate the tween value for this animation
            tween-val (calc-tween-vals (assoc animation :current-time n))
            first-starting-val (first-val start-val)
            first-ending-val (first-val end-val)
            first-tween-val (first-val tween-val)
            ascending? (< first-starting-val first-ending-val)
            time-to-stop? (or
                            ;; we have reached their end-val
                            (if ascending?
                              (>= first-tween-val first-ending-val)
                              (<= first-tween-val first-ending-val))
                            ;; it is past time
                            (> n end-time))]
        ;; run their provided on-tick function with the val
        (when (fn? on-tick)
          (on-tick {:time n
                    :val tween-val}))
        ;; is it time to stop this animation?
        (when time-to-stop?
          (swap! stops conj animation))))

    ;; request a redraw if there are any animations in the animations-queue
    ;; hopefully the user has done something useful with their on-tick function
    ;; so we may see the animation!
    (let [request-frame! @*request-frame-fn]
      (when (and request-frame! (not (empty? queue)))
        (request-frame!)))

    ;; process any stops
    (doseq [{:keys [id on-stop] :as anim} @stops]
      ;; run their on-stop function if they provided one
      (when (fn? on-stop)
        (on-stop (-> anim
                  (assoc :id id)
                  (dissoc :on-stop)
                  (dissoc :on-tick))))
      ;; remove from the animations queue
      (swap! animations-queue dissoc id))))

;; -----------------------------------------------------------------------------
;; Public API

(defn set-request-frame-fn! [f]
  (reset! *request-frame-fn f))

;; TODO: make this vardiadic, accept multiple animations
(defn start-animation!
  [{:keys [start-val end-val duration-ms on-tick transition] :as animation}]
  (let [request-frame! @*request-frame-fn]
    ;; sanity-checks:
    (assert (fn? request-frame!) "Please set the *request-frame-fn before calling start-animation!")
    (when (number? start-val)
      (assert (number? end-val) "start-val and end-val must both be numbers"))
    (when (sequential? start-val)
      (assert (and (sequential? end-val)
                   (= (count start-val) (count end-val))
                   (every? number? start-val)
                   (every? number? end-val))
              "start-val and end-val should be vectors of the same length and contain only numbers"))
    (when (map? start-val)
      (assert (and (map? end-val)
                   (= (keys start-val) (keys end-val))
                   (every? number? (vals start-val))
                   (every? number? (vals end-val)))
              "start-val and end-val must be maps with the same keys and contain only number values"))

    ;; create the animation
    (let [start-time (now)
          end-time (+ start-time duration-ms)
          id (str (random-uuid))
          animation' (assoc animation :id id
                                      :start-time start-time
                                      :end-time end-time
                                      :start-val (convert-vals-to-doubles start-val)
                                      :end-val (convert-vals-to-doubles end-val))]
      ;; add this animation to the queue
      (swap! animations-queue assoc id animation')

      ;; trigger a redraw on the next frame, which should call tick! via the animation-ticker component
      (request-frame!)

      ;; return the id of their new animation
      id)))

;; TODO:
;; - stop-animation!
;; - stop-all-animations!

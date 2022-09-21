(ns humble-animations.main
  (:require
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.window :as window]

    [tween-clj.core :as tween]))

(set! *warn-on-reflection* true)

(reset! debug/*enabled? true)

(def dark-grey 0xff404040)
(def light-grey 0xffeeeeee)
(def blue 0xff0d7fbe)
(def yellow 0xfffae317)
(def red 0xfff50f0f)
(def white 0xfff3f3f3)
(def black 0xff000000)

(def outside-padding 15)
(def inner-padding 10)

(defn now
  "returns the current time in milliseconds"
  []
  (System/currentTimeMillis))

(defonce *window
  (atom nil))

(defn redraw []
  ; (println "redraw!")
  (some-> @*window window/request-frame))

;; TODO: is there an empty component I can use instead of label = "" ?
(defn Box [color]
  (ui/rect (paint/fill color) (ui/label "")))



(def default-rectangle-opts
  {:bg-color red
   :height-px 150
   :width-px 300})

(defn Rectangle2
  ([]
   (Rectangle2 default-rectangle-opts))
  ([{:keys [bg-color height-px width-px] :as _opts}]
   (ui/rect
    (paint/fill bg-color)
    (ui/gap 0 0))))

(def start
  {:x 0})

(def end
  {:x 100})

(def initial-app-state
  {:x 100})

(def *app-state
  (atom initial-app-state))

(defn reset-app-state! []
  (reset! *app-state initial-app-state)
  (redraw))

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

(defn calc-tween-val
  "Calculates a tweening value for a specific time"
  [{:keys [start-val end-val start-time end-time current-time transition]}]
  (let [default-transition-fn (:ease-in transitions)
        shorthand-fn (get transitions transition)
        transition-fn (cond
                        shorthand-fn shorthand-fn
                        (fn? transition) transition
                        :else (do
                                ;; TODO: warning here
                                default-transition-fn))
        p-val (tween/range-to-p start-time end-time current-time)]
    (tween/p-to-range start-val end-val (transition-fn p-val))))

;; roughly 60 frames per second
(def animation-tick-rate-ms
  16)

(def animating? (atom false))

(def animations-queue
  (atom []))

;; TODO:
;; - have animations be an atom: a queue of animation objects
;; - have the "tick" function run constantly and check the animations queue?
;; - assert(start-val < end-val)
;; - assert(duration-ms >= 10)

(defn animation-tick!
  [{:keys [start-val end-val start-time end-time transition on-tick] :as opts}]
  (let [n (now)
        current-val (calc-tween-val (assoc opts :current-time n))
        ; _ (println current-val)
        ascending? (>= end-val start-val)
        ;; TODO:
        ;; - allow them to provide a stop-fn ?
        ;; - stop once we are past duration time?

        ;; stop once the tweening value reaches end-val
        ;; TODO: should we also stop if we are past duration?
        time-to-stop? (if ascending?
                        (>= current-val end-val)
                        (<= current-val end-val))]

    ;; execute their on-tick function with the current tweening value
    (when (fn? on-tick) (on-tick {:time n
                                  :val current-val}))

    ;; perform a redraw
    (redraw)

    ;; queue up the next animation-tick unless it is time to stop
    (when-not time-to-stop?
      (Thread/sleep animation-tick-rate-ms)
      (animation-tick! opts))))

(def animation1
  {:start-val 100
   :end-val 450
   :duration-ms 120
   :on-tick (fn [{:keys [val]}]
              (swap! *app-state assoc :x val))
   :transition :ease-out-sine})

(def animation2
  {:start-val 100
   :end-val 450
   :duration-ms 120
   :on-tick (fn [{:keys [val]}]
              (swap! *app-state assoc :x (- 550 val)))
   :transition :ease-out-sine})

(defn start-animation!
  [{:keys [start-val end-val duration-ms on-tick transition] :as opts}]

  (assert (< start-val end-val) "start-val must be less end-val")

  (let [start-time (now)
        end-time (+ start-time duration-ms)]
  ; (reset! animating? true)
    (animation-tick! (assoc opts :start-time start-time
                                 :end-time end-time))))

(defn stop-animation! []
  (println "Stop animation")
  (reset! animating? false))

(def AirQuotesButtons
  (ui/column
    [:stretch 1 (clickable/clickable
                  {:on-click (fn [evt]
                               (println "Click yellow"))}
                               ; (start-animation! animation1))}
                  (Box yellow))]
    [:stretch 1 (clickable/clickable
                  {:on-click (fn [evt]
                               (println "Click blue"))}
                               ; (start-animation! animation2))}
                  (Box blue))]))

(def HumbleAnimations
  "top-level component"
  (ui/default-theme {}
    (clickable/clickable
      {:on-click
       (fn [evt] nil)}
      (ui/rect (paint/fill light-grey)
        (ui/padding outside-padding
          ; (Box blue)
          (ui/dynamic _ctx [x (:x @*app-state)]
            (ui/row
              [:stretch 1 AirQuotesButtons]
              (ui/gap x 0)
              [:stretch 1 (Box blue)])))))))

        ; (ui/row
        ;   [:stretch 1 (Box blue)]
        ;   (ui/gap inner-padding 0)
        ;   [:stretch 1 (Box yellow)]
        ;   (ui/gap inner-padding 0)
        ;   [:stretch 1 (Rectangle2 {:bg-color dark-grey})])))))

;; re-draw the UI when we load this namespace
(redraw)

(defn -main [& args]
  (reset! *window
    (ui/start-app!
      {:title    "HumbleUI Animations"
       :bg-color 0xFFFFFFFF}
      #'HumbleAnimations)))

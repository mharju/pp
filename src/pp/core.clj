(ns pp.core
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :as str])
  (:import [java.io File]))

;; # Paw patrol member statistics
;; ♪ Paw patrol, paw patrol we'll
;;   be there on the double ♪

;; Just search the slogans from a bunch of subtitles to see who
;; gets a part of the action on that episode.

(def slogans
  {"Chase" #{"Chase is on the case" "These paws uphold the laws" "Spy Chase is on the case" "Super Spy Chase is on the case" "Chase is on the ultimate rescue case" "Mighty Chase is ready to race" "These mighty paws uphold the laws" "Flying ace Chase is on the case" "Moto Chase is on the case"}
   "Marshall" #{"Ready for a ruff ruff rescue" "I'm fired up" "Wooooaah" "Wait for me" "My highly-trained paws are at your service" "I'm okay" "I'm ready for a red hot rescue" "Ready or not, I'm coming in hot" "I'm super fired up" "I'm ultimately fired up" "I'm ready for a high flying rescue" "I'm fired up, moto style" "This knight is fired up to make things right" }
   "Skye" #{"Let's take to the sky" "This pup's gotta fly" "Let's flip to the sky" "This puppy's gotta fly" "This mighty puppy's going for a spin" "Skye the spy is ready to fly" "This Police puppy's gotta fly" "This ultimate puppy's gotta fly" "This fire pup's gotta fly" "This pup doesn't run, she flies" "This moto pup's gonna ride and fly" "Moto Pups, Ready for action Ryder, sir" "This puppy Knights gotta fly" }
  "Rocky" #{"Don't lose it, reuse it" "Why trash it when you can stash it?" "Green means go" "This one pup road crew is gonna make them take a wrong turn" "Green means glow" "Nooooo" "Green means I'm ultimately ready to go" "Don't loose it, remove it" "Let's recycle the trash" "Green means, go moto go" }
  "Rubble" #{"Let's dig it" "Rubble on the double" "Mighty Rubble's ready for trouble" "Let's wreck and roll"  "Time to wreck and roll"  "Rubble on the ultimate double" "Rubble on the fix-it double" "Rubble on the jet speed double" "Rubble on the moto double"}
  "Zuma" #{"Let's dive in" "Ready, set, get wet" "Time to do the wave" "My mighty wave will get the save" "I'm ultimately ready to dive in" "Ready set, let's jet" "Let's drive in"}})

(def subtitles
  (->> (file-seq (File. "subs"))
       (filter #(str/ends-with? % ".srt"))))

(count subtitles)

(defn parse-subtitles [subtitle]
  (->>
    (str/split (slurp subtitle) #"\r\n")
    (filter #(re-find #"[A-Za-z]+" %))
    (str/join " ")
    (str/lower-case)))

(defn pups [s]
  (let [[_ season episode] (re-matches #".*S(\d{2})E(\d{2}).*" (.getName s))
        data (parse-subtitles s)]
    {:season (Integer/parseInt season)
     :episode (Integer/parseInt episode)
     :pups (->> (for [[pup phrases] slogans
                      :when (some #(str/includes? data (str/lower-case %)) phrases)]
                  pup)
                (into []))}))

(def results (->> (map pups subtitles)
                  (sort (fn [{season-a :season episode-a :episode}
                             {season-b :season episode-b :episode}]
                          (if (not= season-a season-b)
                            (compare season-a season-b)
                            (compare episode-a episode-b))))))

(clerk/table results)
(def freqs (->>
             (reduce (fn [acc {:keys [pups]}] (into acc pups)) [] results)
             frequencies
             (map (fn [[pup count]] {:pup pup :count count}))))

(clerk/vl
  {:data {:values freqs}
   :width 600
   :height 400
   :mark "bar"
   :encoding {:x {:field "pup" :sort {:op "sum" :field "count" :order "descending"}}
              :y {:aggregate "sum" :field "count"}}})

;; ## Where is Chase NOT featured?

(clerk/table (remove #(contains? (set (:pups %)) "Chase") results))

(comment
  (require '[nextjournal.clerk :as clerk])
  (clerk/show! "src/pp/core.clj")
  (clerk/serve! {:browse? true :watch-paths ["src"]}))
